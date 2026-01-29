package com.temenos.t24browser.servlets;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.temenos.t24browser.servlets.TCCRequestWrapper.1;
import com.temenos.t24browser.servlets.TCCRequestWrapper.2;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class TCCRequestWrapper extends HttpServletRequestWrapper {
   private final Map modifiableParameters;
   private Map allParameters = null;
   private String webServer = null;
   private String status = "ON";
   private String portNo = null;
   private String messageResponse = " ";
   private String authUserRes1 = null;
   private String authUserRes2 = null;
   private String authUserRes8 = null;
   private String authResError = null;
   private String authResRSAOff = null;
   private String assignPinRes3 = null;
   private String assignPinResOther = null;
   private String stWebURL = null;
   private String stStatus = null;
   private String stPortNo = null;
   private String authUserRes1Para = null;
   private String authUserRes2Para = null;
   private String authUserRes8Para = null;
   private String authResErrorPara = null;
   private String authResRSAOffPara = null;
   private String assignPinRes3Para = null;
   private String assignPinResOtherPara = null;
   private String initUrl = null;
   private String verifyUrl = null;
   private String clientKey = null;

   public TCCRequestWrapper(HttpServletRequest request, Map additionalParams) {
      super(request);
      this.modifiableParameters = new TreeMap();
      this.modifiableParameters.putAll(additionalParams);
      System.out.println("TCCRequestWrapper: Constructor with additionalParams called, params size: " + additionalParams.size());
   }

   public TCCRequestWrapper(HttpServletRequest request) throws UnsupportedEncodingException {
      super(request);
      System.out.println("TCCRequestWrapper: Constructor called, starting passSecurityCheck");
      long startTime = System.currentTimeMillis();
      this.passSecurityCheck();
      System.out.println("TCCRequestWrapper: passSecurityCheck completed in " + (System.currentTimeMillis() - startTime) + "ms");
      startTime = System.currentTimeMillis();
      this.readConfigFile();
      System.out.println("TCCRequestWrapper: readConfigFile completed in " + (System.currentTimeMillis() - startTime) + "ms");
      if (!this.getStatus().equalsIgnoreCase("ON") || this.getWebServer() == null && !this.getWebServer().equalsIgnoreCase("")) {
         if (!this.getStatus().equalsIgnoreCase("OFF") || this.getWebServer() == null && !this.getWebServer().equalsIgnoreCase("")) {
            this.modifiableParameters = new TreeMap();
            startTime = System.currentTimeMillis();
            this.modifiableParameters.putAll(this.getPMap(request));
            System.out.println("TCCRequestWrapper: getPMap called, completed in " + (System.currentTimeMillis() - startTime) + "ms");
         } else {
            this.setMessageResponse(this.getAuthResRSAOff());
            this.modifiableParameters = new TreeMap();
            startTime = System.currentTimeMillis();
            this.modifiableParameters.putAll(this.getRMap(request));
            System.out.println("TCCRequestWrapper: getRMap called, completed in " + (System.currentTimeMillis() - startTime) + "ms");
         }
      } else {
         this.modifiableParameters = new TreeMap();
         startTime = System.currentTimeMillis();
         this.modifiableParameters.putAll(this.getPMap(request));
         System.out.println("TCCRequestWrapper: getPMap called (else branch), completed in " + (System.currentTimeMillis() - startTime) + "ms");
      }
   }

   public void passSecurityCheck() {
      try {
         System.out.println("passSecurityCheck: Starting SSL configuration");
         TrustManager[] trustAllCerts = new TrustManager[]{new 1(this)};
         SSLContext sc = SSLContext.getInstance("SSL");
         sc.init((KeyManager[])null, trustAllCerts, (SecureRandom)null);
         HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
         System.out.println("passSecurityCheck: SSL configuration completed");
      } catch (KeyManagementException var3) {
         System.out.println("passSecurityCheck: KeyManagementException - " + var3.toString());
      } catch (NoSuchAlgorithmException var4) {
         System.out.println("passSecurityCheck: NoSuchAlgorithmException - " + var4.toString());
         Logger.getLogger(TCCRequestWrapper.class.getName()).log(Level.SEVERE, null, var4);
      }
   }

   public Map getPMap(HttpServletRequest req) throws UnsupportedEncodingException {
      long startTime = System.currentTimeMillis();
      System.out.println("getPMap: Starting parameter processing");
      String signName = " ";
      String tokenID = " ";
      String authmodeStr = "";
      String token = "";
      String pin = "";
      String userId = "";
      String authenticateUser = "false";
      Map<Object, Object> m = new HashMap();
      Enumeration<String> e = req.getParameterNames();
      Enumeration e1 = req.getParameterNames();

      int paramCount = 0;
      while(e.hasMoreElements()) {
         String key2 = (String)e.nextElement();
         paramCount++;
         if (key2.equals("signOnName")) {
            signName = req.getParameterValues("signOnName")[0].toString();
            System.out.println("getPMap: Found signOnName = " + signName);
         }
         if (key2.equals("tokenId")) {
            tokenID = req.getParameterValues("tokenId")[0].toString();
            System.out.println("getPMap: Found tokenId = " + tokenID);
         }
         if (key2.equals("authmode")) {
            authmodeStr = req.getParameterValues("authmode")[0].toString();
            System.out.println("getPMap: Found authmode = " + authmodeStr);
         }
         if (key2.equals("token")) {
            token = req.getParameterValues("token")[0].toString();
            System.out.println("getPMap: Found token = " + token);
         }
         if (key2.equals("userId")) {
            userId = req.getParameterValues("userId")[0].toString();
            System.out.println("getPMap: Found userId = " + userId);
         }
         if (key2.equals("pin")) {
            pin = req.getParameterValues("pin")[0].toString();
            System.out.println("getPMap: Found pin = " + pin);
         }
      }
      System.out.println("getPMap: Processed " + paramCount + " parameters in " + (System.currentTimeMillis() - startTime) + "ms");

      if (authmodeStr.equalsIgnoreCase("newpin")) {
         System.out.println("getPMap: Calling validateTokenCreateNewPin for userId: " + userId);
         startTime = System.currentTimeMillis();
         this.validateTokenCreateNewPin(userId, token, pin);
         System.out.println("getPMap: validateTokenCreateNewPin completed in " + (System.currentTimeMillis() - startTime) + "ms");
      } else if (signName.length() != 1) {
         System.out.println("getPMap: Calling initTokenAuthenticateUserForRSA for signName: " + signName);
         startTime = System.currentTimeMillis();
         authenticateUser = this.initTokenAuthenticateUserForRSA(signName, tokenID);
         System.out.println("getPMap: initTokenAuthenticateUserForRSA completed in " + (System.currentTimeMillis() - startTime) + "ms, result: " + authenticateUser);

         paramCount = 0;
         while(e1.hasMoreElements()) {
            String key2 = (String)e1.nextElement();
            paramCount++;
            String[] arrayOfString1;
            if (key2.equals("password")) {
               if (authenticateUser.equalsIgnoreCase("false")) {
                  arrayOfString1 = new String[]{" "};
                  m.put(key2, arrayOfString1);
                  System.out.println("getPMap: Password set to blank due to failed authentication");
               } else if (authenticateUser.equalsIgnoreCase("true")) {
                  arrayOfString1 = new String[]{req.getParameterValues(key2)[0].toString()};
                  m.put(key2, arrayOfString1);
                  System.out.println("getPMap: Password retained after successful authentication");
               } else {
                  arrayOfString1 = new String[]{" "};
                  m.put(key2, arrayOfString1);
                  System.out.println("getPMap: Password set to blank for unknown authentication state");
               }
            } else {
               arrayOfString1 = req.getParameterValues(key2);
               m.put(key2, arrayOfString1);
            }
         }
         System.out.println("getPMap: Processed " + paramCount + " parameters in second loop");
      }
      System.out.println("getPMap: Completed, map size: " + m.size());
      return m;
   }

   public Map getRMap(HttpServletRequest req) {
      long startTime = System.currentTimeMillis();
      System.out.println("getRMap: Starting parameter processing");
      Map<Object, Object> m = new HashMap();
      Enumeration e1 = req.getParameterNames();

      int paramCount = 0;
      while(e1.hasMoreElements()) {
         String key = (String)e1.nextElement();
         String[] val = req.getParameterValues(key);
         m.put(key, val);
         paramCount++;
      }
      System.out.println("getRMap: Processed " + paramCount + " parameters in " + (System.currentTimeMillis() - startTime) + "ms");
      return m;
   }

   public void readConfigFile() {
      long startTime = System.currentTimeMillis();
      System.out.println("readConfigFile: Starting configuration loading");
      try {
         this.setStWebURL(System.getProperty("web.server.ip").trim());
         this.setInitUrl(System.getProperty("init.server.url").trim());
         this.setVerifyUrl(System.getProperty("verify.server.url").trim());
         this.setClientKey(System.getProperty("client.key").trim());
         this.setStStatus(System.getProperty("auth.status").trim());
         this.setStPortNo(System.getProperty("port.no").trim());
         this.setAuthUserRes1Para(System.getProperty("authenticate.user.response1").trim());
         this.setAuthUserRes2Para(System.getProperty("authenticate.user.response2").trim());
         this.setAuthUserRes8Para(System.getProperty("authenticate.user.response8").trim());
         this.setAuthResErrorPara(System.getProperty("authentication.response.error").trim());
         this.setAuthResRSAOffPara(System.getProperty("authentication.response.rsa.off").trim());
         this.setAssignPinRes3Para(System.getProperty("assign.user.response3").trim());
         this.setAssignPinResOtherPara(System.getProperty("assign.user.response.other").trim());
         System.out.println("readConfigFile: System properties loaded");

         if (this.initUrl != null && !this.initUrl.equalsIgnoreCase("")) {
            this.setWebServer(this.initUrl);
            System.out.println("readConfigFile: WebServer set to initUrl: " + this.initUrl);
         } else {
            this.setWebServer("localhost");
            System.out.println("readConfigFile: WebServer set to default: localhost");
         }

         if (this.stStatus != null && !this.stStatus.equalsIgnoreCase("")) {
            this.setStatus(this.stStatus);
            System.out.println("readConfigFile: Status set to: " + this.stStatus);
         } else {
            this.setStatus("ON");
            System.out.println("readConfigFile: Status set to default: ON");
         }

         if (this.stPortNo != null && !this.stPortNo.equalsIgnoreCase("")) {
            this.setPortNo(this.stPortNo);
            System.out.println("readConfigFile: PortNo set to: " + this.stPortNo);
         } else {
            this.setPortNo("8080");
            System.out.println("readConfigFile: PortNo set to default: 8080");
         }

         // Similar logging for other configuration settings
         System.out.println("readConfigFile: AuthUserRes1 set to: " + (this.authUserRes1Para != null ? this.authUserRes1Para : "default"));
         System.out.println("readConfigFile: AuthUserRes2 set to: " + (this.authUserRes2Para != null ? this.authUserRes2Para : "default"));
         System.out.println("readConfigFile: AuthUserRes8 set to: " + (this.authUserRes8Para != null ? this.authUserRes8Para : "default"));
         System.out.println("readConfigFile: AuthResError set to: " + (this.authResErrorPara != null ? this.authResErrorPara : "default"));
         System.out.println("readConfigFile: AuthResRSAOff set to: " + (this.authResRSAOffPara != null ? this.authResRSAOffPara : "default"));
         System.out.println("readConfigFile: AssignPinRes3 set to: " + (this.assignPinRes3Para != null ? this.assignPinRes3Para : "default"));
         System.out.println("readConfigFile: AssignPinResOther set to: " + (this.assignPinResOtherPara != null ? this.assignPinResOtherPara : "default"));
      } catch (NullPointerException var2) {
         System.out.println("readConfigFile: NullPointerException - " + var2.getMessage());
         this.setWebServer("localhost");
         this.setStatus("ON");
         this.setPortNo("8080");
         var2.printStackTrace();
      }
      System.out.println("readConfigFile: Completed in " + (System.currentTimeMillis() - startTime) + "ms");
   }

   public String initTokenAuthenticateUserForRSA(String userId, String passcode) {
      long startTime = System.currentTimeMillis();
      System.out.println("initTokenAuthenticateUserForRSA: Starting for userId: " + userId);
      String finalresponse = "";
      Gson gson = new Gson();

      try {
         JSONObject contextReq = new JSONObject();
         JSONObject rootJson = new JSONObject();
         contextReq.put("messageId", UUID.randomUUID().toString());
         rootJson.put("authnAttemptTimeout", 180);
         rootJson.put("subjectName", userId);
         rootJson.put("lang", "us_EN");
         rootJson.put("authMethodId", "TOKEN");
         rootJson.put("context", contextReq);
         System.out.println("initTokenAuthenticateUserForRSA: JSON prepared - " + rootJson.toJSONString());

         startTime = System.currentTimeMillis();
         String webresponse = this.fetchResponseFromRSA(rootJson.toJSONString(), this.getInitUrl());
         System.out.println("initTokenAuthenticateUserForRSA: fetchResponseFromRSA completed in " + (System.currentTimeMillis() - startTime) + "ms");

         if (webresponse != null) {
            System.out.println("initTokenAuthenticateUserForRSA: Parsing response");
            com.temenos.t24browser.servlets.TCCRequestWrapper.ResponseRootJSON resresponse = (com.temenos.t24browser.servlets.TCCRequestWrapper.ResponseRootJSON)gson.fromJson(webresponse, com.temenos.t24browser.servlets.TCCRequestWrapper.ResponseRootJSON.class);
            if (!"FAIL".equals(resresponse.getAttemptResponseCode())) {
               startTime = System.currentTimeMillis();
               finalresponse = this.verifyTokenAuthenticateUserForRSA(webresponse, passcode);
               System.out.println("initTokenAuthenticateUserForRSA: verifyTokenAuthenticateUserForRSA completed in " + (System.currentTimeMillis() - startTime) + "ms");
            } else {
               finalresponse = "false";
               System.out.println("initTokenAuthenticateUserForRSA: Response code FAIL");
            }
         } else {
            finalresponse = "false";
            System.out.println("initTokenAuthenticateUserForRSA: Web response is null");
         }
      } catch (Exception var9) {
         System.out.println("initTokenAuthenticateUserForRSA: Exception - " + var9.getMessage());
         finalresponse = "false";
      }

      System.out.println("initTokenAuthenticateUserForRSA: Completed in " + (System.currentTimeMillis() - startTime) + "ms, finalresponse: " + finalresponse);
      return finalresponse;
   }

   public String verifyTokenAuthenticateUserForRSA(String responseString, String passcode) {
      long startTime = System.currentTimeMillis();
      System.out.println("verifyTokenAuthenticateUserForRSA: Starting");
      String finalresponse = "";
      Gson gson = new Gson();

      try {
         System.out.println("verifyTokenAuthenticateUserForRSA: Parsing response string");
         com.temenos.t24browser.servlets.TCCRequestWrapper.ResponseRootJSON rresponse = (com.temenos.t24browser.servlets.TCCRequestWrapper.ResponseRootJSON)gson.fromJson(responseString, com.temenos.t24browser.servlets.TCCRequestWrapper.ResponseRootJSON.class);
         com.temenos.t24browser.servlets.TCCRequestWrapper.rootJson rj = new com.temenos.t24browser.servlets.TCCRequestWrapper.rootJson(this);
         com.temenos.t24browser.servlets.TCCRequestWrapper.context cc = new com.temenos.t24browser.servlets.TCCRequestWrapper.context(this);
         com.temenos.t24browser.servlets.TCCRequestWrapper.subjectCredentials ss = new com.temenos.t24browser.servlets.TCCRequestWrapper.subjectCredentials(this);
         com.temenos.t24browser.servlets.TCCRequestWrapper.collectedInputs cci = new com.temenos.t24browser.servlets.TCCRequestWrapper.collectedInputs(this);
         cc.setAuthnAttemptId(rresponse.getContext().getAuthnAttemptId());
         cc.setInResponseTo(rresponse.getContext().getMessageId());
         cc.setMessageId(((com.temenos.t24browser.servlets.TCCRequestWrapper.Challenge)rresponse.getChallengeMethods().getChallenges().get(0)).getMethodSetId());
         rj.setContext(cc);
         ss.setMethodId("TOKEN");
         ss.setVersionId("TemenosTransact");
         cci.setName("TOKEN");
         cci.setValue(passcode);
         ArrayList<com.temenos.t24browser.servlets.TCCRequestWrapper.collectedInputs> ccis = new ArrayList();
         ccis.add(cci);
         ss.setCollectedInputs(ccis);
         ArrayList<com.temenos.t24browser.servlets.TCCRequestWrapper.subjectCredentials> ssc = new ArrayList();
         ssc.add(ss);
         rj.setSubjectCredentials(ssc);
         System.out.println("verifyTokenAuthenticateUserForRSA: JSON prepared for verification");

         startTime = System.currentTimeMillis();
         String clientresponse = this.fetchResponseFromRSA(gson.toJson(rj), this.getVerifyUrl());
         System.out.println("verifyTokenAuthenticateUserForRSA: fetchResponseFromRSA completed in " + (System.currentTimeMillis() - startTime) + "ms");

         if (clientresponse != null) {
            System.out.println("verifyTokenAuthenticateUserForRSA: Parsing client response");
            com.temenos.t24browser.servlets.TCCRequestWrapper.ResponseRootJSON resresponse = (com.temenos.t24browser.servlets.TCCRequestWrapper.ResponseRootJSON)gson.fromJson(clientresponse, com.temenos.t24browser.servlets.TCCRequestWrapper.ResponseRootJSON.class);
            if ("SUCCESS".equals(resresponse.getAttemptResponseCode())) {
               finalresponse = "true";
               System.out.println("verifyTokenAuthenticateUserForRSA: Authentication SUCCESS");
            } else {
               finalresponse = "false";
               System.out.println("verifyTokenAuthenticateUserForRSA: Authentication FAILED, response code: " + resresponse.getAttemptResponseCode());
            }
         } else {
            finalresponse = "false";
            System.out.println("verifyTokenAuthenticateUserForRSA: Client response is null");
         }
      } catch (Exception var14) {
         System.out.println("verifyTokenAuthenticateUserForRSA: Exception - " + var14.getMessage());
         finalresponse = "false";
      }

      System.out.println("verifyTokenAuthenticateUserForRSA: Completed in " + (System.currentTimeMillis() - startTime) + "ms, finalresponse: " + finalresponse);
      return finalresponse;
   }

   public String fetchResponseFromRSA(String payload, String url) {
      long startTime = System.currentTimeMillis();
      System.out.println("fetchResponseFromRSA: Starting with URL: " + url);
      String finalstring = null;

      try {
         URL ccurl = new URL(url);
         HttpsURLConnection connection = (HttpsURLConnection)ccurl.openConnection();
         connection.setDoOutput(true);
         connection.setInstanceFollowRedirects(false);
         connection.setRequestMethod("POST");
         connection.setRequestProperty("Accept", "application/json");
         connection.setRequestProperty("Content-Type", "application/json");
         connection.setRequestProperty("client-key", this.getClientKey());
         connection.setHostnameVerifier(new 2(this));
         System.out.println("fetchResponseFromRSA: HTTP connection configured");

         startTime = System.currentTimeMillis();
         OutputStream os = connection.getOutputStream();
         os.write(payload.getBytes("UTF8"));
         os.flush();
         System.out.println("fetchResponseFromRSA: Payload sent in " + (System.currentTimeMillis() - startTime) + "ms");

         int responseCode = connection.getResponseCode();
         System.out.println("fetchResponseFromRSA: Response Code " + responseCode);

         BufferedReader br;
         StringBuilder sb;
         String output;
         if (200 <= responseCode && responseCode <= 299) {
            if (connection.getInputStream() != null) {
               startTime = System.currentTimeMillis();
               br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
               sb = new StringBuilder();
               while((output = br.readLine()) != null) {
                  sb.append(output);
               }
               finalstring = sb.toString();
               System.out.println("fetchResponseFromRSA: Response read in " + (System.currentTimeMillis() - startTime) + "ms");
            }
         } else if (connection.getErrorStream() != null) {
            startTime = System.currentTimeMillis();
            br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            sb = new StringBuilder();
            while((output = br.readLine()) != null) {
               sb.append(output);
            }
            finalstring = sb.toString();
            System.out.println("fetchResponseFromRSA: Error stream read in " + (System.currentTimeMillis() - startTime) + "ms");
         }
      } catch (Exception var11) {
         System.out.println("fetchResponseFromRSA: Exception - " + var11.getMessage());
      }

      System.out.println("fetchResponseFromRSA: Completed in " + (System.currentTimeMillis() - startTime) + "ms, response length: " + (finalstring != null ? finalstring.length() : 0));
      return finalstring;
   }

   public String validateTokenAuthenticateUser(String userId, String passcode) throws UnsupportedEncodingException {
      long startTime = System.currentTimeMillis();
      System.out.println("validateTokenAuthenticateUser: Starting for userId: " + userId);
      String AUTHENTICATE_USER = "http://172.24.52.193:8080/authManagerTest/sop.jsp";
      String AUTHENTICATE_USER1 = this.getWebServer();
      String responseMessage = "FALSE";

      try {
         JSONObject authrequest = new JSONObject();
         authrequest.put("userid", userId);
         authrequest.put("password", passcode);
         System.out.println("validateTokenAuthenticateUser: JSON request prepared");

         Client client = Client.create();
         client.addFilter(new GZIPContentEncodingFilter(true));
         client.addFilter(new LoggingFilter(Logger.getLogger(this.getClass().getSimpleName())));
         WebResource webResource = client.resource(AUTHENTICATE_USER1);
         Builder brequest = webResource.accept(new String[]{"application/json"});
         String deviceIPAddress = InetAddress.getLocalHost().getHostAddress();
         brequest.header("Accept-Encoding", "gzip,deflate");
         brequest.header("Content-Encoding", "gzip");
         brequest.header("Content-Language", "en-US,en;q=0.5");
         brequest.header("Cache-Control", "nocache");
         brequest.header("Content-Type", "application/json");
         System.out.println("validateTokenAuthenticateUser: HTTP request configured");

         startTime = System.currentTimeMillis();
         ClientResponse webresponse = (ClientResponse)brequest.post(ClientResponse.class, authrequest.toJSONString());
         System.out.println("validateTokenAuthenticateUser: POST request completed in " + (System.currentTimeMillis() - startTime) + "ms");

         if (webresponse.getStatus() == 200) {
            String responseString = (String)webresponse.getEntity(String.class);
            System.out.println("validateTokenAuthenticateUser: Response received, length: " + responseString.length());

            try {
               JSONParser parser = new JSONParser();
               JSONObject response = (JSONObject)parser.parse(responseString);
               System.out.println("validateTokenAuthenticateUser: Response parsed");
               if (response.get("responseCode") != null) {
                  System.out.println("validateTokenAuthenticateUser: Response code: " + response.get("responseCode"));
               } else {
                  response.put("responseCode", "0");
                  response.put("responseDesc", "INVALID SERVER RESPONSE RECEIVED.");
                  System.out.println("validateTokenAuthenticateUser: INVALID SERVER RESPONSE RECEIVED");
               }
            } catch (Exception var16) {
               System.out.println("validateTokenAuthenticateUser: Parse Exception - " + var16.getMessage());
               JSONObject response = new JSONObject();
               response.put("responseCode", "0");
               response.put("responseDesc", "INVALID SERVER RESPONSE RECEIVED");
            }
         } else {
            System.out.println("validateTokenAuthenticateUser: Server error, status: " + webresponse.getStatus());
            JSONObject response = new JSONObject();
            response.put("responseCode", "0");
            response.put("responseDesc", "SERVER UNREACHABLE.PLEASE CONTACT YOUR SYSTEMADMINISTRATOR.");
         }

         JSONObject response = (JSONObject) new JSONParser().parse(webresponse.getEntity(String.class));
         if (response != null) {
            String responseCode = String.valueOf(response.get("responseCode"));
            System.out.println("validateTokenAuthenticateUser: Processing response code: " + responseCode);
            if (responseCode.equals("1")) {
               responseMessage = "TRUE";
               this.setMessageResponse(this.getAuthUserRes1());
            } else if (responseCode.equals("0")) {
               responseMessage = "FALSE";
               this.setMessageResponse(this.getAuthUserRes2());
            } else if (responseCode.equals("8")) {
               responseMessage = "FALSE";
               this.setMessageResponse(this.getAuthUserRes8());
            } else {
               responseMessage = "ERROR";
               this.setMessageResponse(this.getAuthResError());
            }
         }
      } catch (Exception var17) {
         System.out.println("validateTokenAuthenticateUser: Exception - " + var17.getMessage());
         responseMessage = "ERROR";
         this.setMessageResponse(this.getAuthResError());
      }

      System.out.println("validateTokenAuthenticateUser: Completed in " + (System.currentTimeMillis() - startTime) + "ms, response: " + responseMessage);
      return responseMessage;
   }

   public String validateTokenCreateNewPin(String userId, String code, String pin) {
      long startTime = System.currentTimeMillis();
      System.out.println("validateTokenCreateNewPin: Starting for userId: " + userId);
      String responseMessage = "FALSE";

      try {
         Client client = Client.create();
         String url = "http://" + this.getWebServer() + ":" + this.getPortNo() + "/CBN_RSAV2/auth/service/assignPin/" + userId.trim() + "," + code.trim() + "," + pin.trim();
         System.out.println("validateTokenCreateNewPin: Request URL: " + url);
         WebResource wbrSource = client.resource(url);
         startTime = System.currentTimeMillis();
         ClientResponse resp = (ClientResponse)wbrSource.accept(new String[]{"application/json"}).get(ClientResponse.class);
         System.out.println("validateTokenCreateNewPin: GET request completed in " + (System.currentTimeMillis() - startTime) + "ms");
         String rp = (String)resp.getEntity(String.class);
         System.out.println("validateTokenCreateNewPin: Response: " + rp);
         if (rp.equals("3")) {
            responseMessage = "TRUE";
            this.setMessageResponse(this.getAssignPinRes3());
         } else {
            responseMessage = "FALSE";
            this.setMessageResponse(this.getAssignPinResOther());
         }
      } catch (Exception var10) {
         System.out.println("validateTokenCreateNewPin: Exception - " + var10.getMessage());
         responseMessage = "ERROR";
         this.setMessageResponse(this.getAuthResError());
      }

      System.out.println("validateTokenCreateNewPin: Completed in " + (System.currentTimeMillis() - startTime) + "ms, response: " + responseMessage);
      return responseMessage;
   }

   // Getter and setter methods remain unchanged as they are unlikely to be bottlenecks
   // ... (all getter and setter methods as in the original code)
}