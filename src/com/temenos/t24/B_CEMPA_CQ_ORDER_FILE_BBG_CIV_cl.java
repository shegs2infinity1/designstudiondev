package com.temenos.t24;

import com.temenos.tafj.common.exception.JBCCatchableException;
import com.temenos.tafj.common.exception.NeedRestartException;
import com.temenos.tafj.common.jSession;
import com.temenos.tafj.common.jSystem;
import com.temenos.tafj.common.jVar;
import com.temenos.tafj.common.jVarFactory;
import com.temenos.tafj.runtime.jAtVariable;
import com.temenos.tafj.runtime.jRunTime;
import java.lang.reflect.Field;

public class B_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl extends jRunTime {
  private boolean _inMove_ = false;
  
  private boolean _commonWasNull = false;
  
  private boolean _isBreak_ = false;
  
  private boolean _isContinue_ = false;
  
  private boolean _loop_ = true;
  
  private boolean _NeedInitialise_ = true;
  
  private String[] _varList_ = null;
  
  private String[] _componentList_ = null;
  
  private static String[] _paramList_ = null;
  
  public jVar _PASSED_CHAR;
  
  public jVar _Y_FIRST_LINE_FLAG;
  
  public jVar _F_CHQ_ISSUE_HIS_BBG_SN;
  
  public jVar _F_CHQ_ISSUE_FAILED_BBG_SN;
  
  public jVar _DATA_FILE_ID;
  
  public jVar _N_BARRE_NON;
  
  public jVar _CHQ_ADDR3;
  
  public jVar _CI_ADD2_POS;
  
  public jVar _CHQ_ADDR2;
  
  public jVar _CHQ_ADDR1;
  
  public jVar _S_PARAM_NAMES;
  
  public jVar _FILE_ID;
  
  public jVar _Y_CQ_BOOK_OUT_PATH;
  
  public jVar _CI_TEL_NO_POS;
  
  public jVar _OFS_MSG_ID;
  
  public jVar _Y_LETTRE_CHEQUE_TYPES;
  
  public jVar _FN_ACCOUNT;
  
  public jVar _APP_LIST;
  
  public jVar _CI_NO;
  
  public jVar _N_FILE_D_REC_ID;
  
  public jVar _STR_POS;
  
  public jVar _CLE_RIB_A;
  
  public jVar _Y_COM_MNEMONIC;
  
  public jVar _CIT_ID;
  
  public jVar _CI_ADD1_POS;
  
  public jVar _Y_PARAM_IDS;
  
  public jVar _S_PARAM_ID;
  
  public jVar _Y_SEQ_NO_OLD;
  
  public jVar _RIB_NUM;
  
  public jVar _Y_CHQ_NO_LEAVES_AH;
  
  public jVar _F_CHQ_ISSUE_TODAY_BBG_SN;
  
  public jVar _AC_TYPE_POS;
  
  public long _PROCESS_GO;
  
  public jVar _CI_COLL_CTR_POS;
  
  public jVar _CI_NAME_POS;
  
  public jVar _FMT_RIB;
  
  public jVar _R_ACCOUNT;
  
  public jVar _N_ORDER_DATE;
  
  public jVar _N_BANK_CODE;
  
  public jVar _COM_ERR;
  
  public jVar _Y_RETURN_CODE;
  
  public jVar _CIH_ID;
  
  public jVar _Y_CI_OFS_VERSION;
  
  public jVar _R_CASN;
  
  public jVar _F_CHQ_ISSUE_SEQ_BBG_SN;
  
  public jVar _N_FILE_H_REC_ID;
  
  public jVar _N_EMPTY;
  
  public jVar _F_CHEQUE_ISSUE;
  
  public jVar _CI_QUANTITY;
  
  public jVar _Y_CHQ_NO_LEAVES_H;
  
  public jVar _CLE_RIB;
  
  public jVar _Y_PARAM_NAMES;
  
  public jVar _Y_CQ_PROCESSED_STATUS;
  
  public jVar _CI_ID;
  
  public jVar _COMP_NAME;
  
  public jVar _Y_EXT_ID;
  
  public jVar _N_vignettes;
  
  public jVar _CI_NO_LEAVES_POS;
  
  public jVar _N_Country_Code;
  
  public jVar _FN_COMPANY;
  
  public jVar _CIH_ERR;
  
  public jVar _F_CHQ_ACCT_SERIAL_NO_BBG_SN;
  
  public jVar _R_COM;
  
  public jVar _sPos;
  
  public jVar _FN_CHQ_ISSUE_HIS_BBG_SN;
  
  public jVar _N_R_LINE_DETAIL;
  
  public jVar _CI_PRINT_POS;
  
  public jVar _Y_FILE_UPDATED;
  
  public jVar _POS;
  
  public jVar _Y_CHEQUE_DESCRIPTION;
  
  public jVar _Y_EXT_ID_LIST;
  
  public jVar _Y_PARAM_VALUES;
  
  public jVar _REC_LOCK_ERR;
  
  public jVar _R_COMPANY;
  
  public jVar _AC_ID;
  
  public jVar _N_VAL_DEF;
  
  public jVar _N_R_FINAL_ARRAY;
  
  public jVar _R_INT_EXT;
  
  public jVar _COMP_ADDR4;
  
  public jVar _COMP_ADDR3;
  
  public jVar _COMP_ADDR2;
  
  public jVar _COMP_ADDR1;
  
  public jVar _N_ORDER_SEQ_NO;
  
  public jVar _F_ACCOUNT;
  
  public jVar _F_CHQ_INTERFACE_EXTRACT_BBG_SN;
  
  public jVar _FN_CHQ_INTERFACE_EXTRACT_BBG_SN;
  
  public jVar _CODE_GUICHET;
  
  public jVar _CI_CROSSING_POS;
  
  public jVar _FN_CHEQUE_ISSUE;
  
  public jVar _FN_LOCKING;
  
  public jVar _CI_CNT;
  
  public jVar _PASSED_NO;
  
  public jVar _FN_CHQ_ISSUE_FAILED_BBG_SN;
  
  public jVar _SELECT_LIST;
  
  public jVar _N_BBG_GROUP;
  
  public jVar _FN_CHQ_ACCT_SERIAL_NO_BBG_SN;
  
  public jVar _CIT_ERR;
  
  public jVar _F_CHQ_DATA_FILE_BBG_SN;
  
  public jVar _AC_ERR;
  
  public jVar _N_CROSS_CHQ;
  
  public jVar _N_MODEL_CODE;
  
  public jVar _Y_FILE_NAME_PREFIX;
  
  public jVar _R_DATA_FILE;
  
  public jVar _R_CI;
  
  public jVar _FLD_LIST;
  
  public jVar _CIS_ERR;
  
  public jVar _Y_CHQ_NO_LEAVES;
  
  public jVar _N_BBG_GROUP_CI;
  
  public jVar _R_CIT;
  
  public jVar _FN_CHQ_ISSUE_SEQ_BBG_SN;
  
  public jVar _R_CIH;
  
  public jVar _FLD_POS;
  
  public jVar _N_BARRE_NO_EXT;
  
  public jVar _RUNNING_IN_JBASE;
  
  public jVar _RET_CODE;
  
  public jVar _FN_CHQ_ISSUE_TODAY_BBG_SN;
  
  public jVar _F_LOCKING_CHQ;
  
  public jVar _N_branch_Code;
  
  public jVar _R_LOCK_T;
  
  public jVar _REQ_COMMITTED;
  
  public jVar _OFS_RESPONSE;
  
  public jVar _N_FILE_H_BEGIN_ID;
  
  public jVar _CI_PID_POS;
  
  public jVar _Y_CI_INDEX_SEQ_NO;
  
  public jVar _R_LOCK;
  
  public jVar _COMP_ID;
  
  public jVar _S_PARAM_VALUES;
  
  public jVar _N_Endossable;
  
  public jVar _TODAY;
  
  public jVar _NAME_ON_CHQ;
  
  public jVar _F_COMPANY;
  
  public jVar _N_CHQ_IN;
  
  public jVar _COMP_ADDR;
  
  public jVar _Y_CI_INDEX_LOCK_ID;
  
  public jVar _ERR_LOCKING;
  
  public jVar _Y_CHEQUE_TYPE;
  
  public jVar _NO_OF_RECS;
  
  public jVar _CI_ERR;
  
  public jVar _Y_LOCK_ID;
  
  public jVar _N_OLD_CHQ_TYPE;
  
  public jVar _Y_SEQ_NO;
  
  public jVar _CI_ACCT_POS;
  
  public jVar _PRINTER_ID;
  
  public jVar _Y_OFS_SOURCE_ID;
  
  public jVar _OFS_REC;
  
  public jVar _Y_CQ_PROCESS_STATUS;
  
  public jVar _S_RETURN_CODE;
  
  public jVar _CI_CQ_START_POS;
  
  public jVar _CI_ADD3_POS;
  
  public jVar _Y_SR_NO;
  
  public jVar _SELECT_CMD;
  
  public jVar _F_FILE_OUT_DIR;
  
  public jVar _Y_CHQ_TYPE_LIST;
  
  public jVar _FN_CHQ_DATA_FILE_BBG_SN;
  
  public jVar _CHQ_TELNO;
  
  protected static final int LABEL_EXIT = -3;
  
  protected static final int LABEL_STOP = -2;
  
  protected static final int LABEL_NULL = -1;
  
  protected static final int main = 0;
  
  protected static final int lbl_GET_PARAM_VALUES = 1;
  
  protected static final int lbl_POST_OFS_BULK = 2;
  
  protected static final int lbl_BOOK_OUT_PATH = 3;
  
  protected static final int lbl_UPDATE_CONCAT_HISTORY = 4;
  
  protected static final int lbl_GET_CHEQUE_DESCRIPTION = 7;
  
  protected static final int lbl_GET_ACCOUNT_RIB = 9;
  
  protected static final int lbl_FIND_CROSS_CHECK_ENDO = 10;
  
  protected static final int lbl_UPDATE_DATA_FILE = 11;
  
  protected static final int lbl_GET_CHQ_SEQUENCE_NUMBER = 12;
  
  protected static final int lbl_GET_COMPANY_NAME = 13;
  
  protected static final int lbl_UPDATE_CHEQUE_ISSUE = 14;
  
  protected static final int lbl_OPENFILES = 15;
  
  protected static final int lbl_UPDATE_HEADER_DET = 16;
  
  protected static final int lbl_MAIN_PROCESS = 17;
  
  protected static final int lbl_GET_CHEQUE_ISSUE_DETAIL = 18;
  
  protected static final int lbl_INITIALISE = 19;
  
  public void keepMoving() {
    this._inMove_ = true;
    while (this._inMove_)
      move(); 
  }
  
  public void move() {
    try {
      Thread.sleep(200L);
    } catch (InterruptedException interruptedException) {}
  }
  
  public void stopMoving() {
    this._inMove_ = false;
  }
  
  protected int main() {
    _l(25, true);
    INSERT__I__COMMON();
    _l(26, true);
    INSERT__I__EQUATE();
    _l(27, true);
    INSERT__I__F_CHEQUE_ISSUE();
    _l(28, true);
    INSERT__I__F_ACCOUNT();
    _l(29, true);
    INSERT__I__F_COMPANY();
    _l(30, true);
    INSERT__I__F_LOCKING();
    _l(31, true);
    INSERT__I__F_EB_INTERFACE_EXTRACT_BBG_SN();
    _l(33, 180);
    this._Sys_PostGlobus = lbl_INITIALISE();
    if (this._Sys_PostGlobus != -1)
      return this._Sys_PostGlobus; 
    _l(34, 180);
    this._Sys_PostGlobus = lbl_OPENFILES();
    if (this._Sys_PostGlobus != -1)
      return this._Sys_PostGlobus; 
    _l(35, 180);
    this._Sys_PostGlobus = lbl_GET_PARAM_VALUES();
    if (this._Sys_PostGlobus != -1)
      return this._Sys_PostGlobus; 
    _l(36, 180);
    this._Sys_PostGlobus = lbl_GET_CHQ_SEQUENCE_NUMBER();
    if (this._Sys_PostGlobus != -1)
      return this._Sys_PostGlobus; 
    _l(37, 180);
    this._Sys_PostGlobus = lbl_MAIN_PROCESS();
    if (this._Sys_PostGlobus != -1)
      return this._Sys_PostGlobus; 
    _l(39, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_MAIN_PROCESS() {
    _l(44);
    set(this._SELECT_CMD, op_cat("SELECT ", this._FN_CHQ_ISSUE_TODAY_BBG_SN));
    _l(45);
    set(this._SELECT_LIST, "");
    _l(46);
    set(this._RET_CODE, "");
    _l(47);
    set(this._NO_OF_RECS, "");
    _l(48);
    set(this._N_ORDER_SEQ_NO, 1L);
    _l(49);
    set(this._N_ORDER_SEQ_NO, FMT(this._N_ORDER_SEQ_NO, "R%5"));
    _l(51);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "EB.READLIST" }).invoke(new Object[] { this._SELECT_CMD, this._SELECT_LIST, "", this._NO_OF_RECS, this._RET_CODE });
    } else {
      EB_READLIST_cl.INSTANCE(this.session).invoke(new Object[] { this._SELECT_CMD, this._SELECT_LIST, "", this._NO_OF_RECS, this._RET_CODE });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(53);
    set(this._N_CHQ_IN, 1L);
    _l(54);
    set(this._N_OLD_CHQ_TYPE, "");
    _l(55);
    set(this._Y_CHEQUE_TYPE, "");
    _l(56);
    set(this._Y_FIRST_LINE_FLAG, "");
    _l(58);
    do {
      this._isContinue_ = false;
      _l(59);
      if (this._isBreak_ || !boolVal(op_le(this._N_CHQ_IN, this._NO_OF_RECS)))
        break; 
      _l(60);
      set(this._R_CIT, "");
      set(this._CIT_ERR, "");
      _l(61);
      set(this._CIT_ID, get(this._SELECT_LIST, this._N_CHQ_IN, 0, 0));
      _l(62);
      if (this.session.isUnitTest()) {
        this.session.findStub(new String[] { "F.READ" }).invoke(new Object[] { this._FN_CHQ_ISSUE_TODAY_BBG_SN, this._CIT_ID, this._R_CIT, this._F_CHQ_ISSUE_TODAY_BBG_SN, this._CIT_ERR });
      } else {
        F_READ_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_ISSUE_TODAY_BBG_SN, this._CIT_ID, this._R_CIT, this._F_CHQ_ISSUE_TODAY_BBG_SN, this._CIT_ERR });
      } 
      if (this.session.getStateSubroutineAfterCall() == -3)
        return -3; 
      _l(64);
      set(this._CI_NO, DCOUNT(this._R_CIT, Character.valueOf(jAtVariable.VM)));
      _l(66);
      set(this._CI_CNT, 1L);
      _l(67);
      set(this._R_DATA_FILE, "");
      _l(68);
      set(this._Y_CHEQUE_TYPE, LEFT(this._CIT_ID, 4));
      _l(69);
      set(this._Y_CHQ_NO_LEAVES, FIELD(this._CIT_ID, ".", Integer.valueOf(2)));
      _l(72);
      set(this._Y_CHQ_NO_LEAVES_AH, FMT(this._Y_CHQ_NO_LEAVES, "R#6"));
      _l(73);
      set(this._Y_CHQ_NO_LEAVES_H, FMT(this._Y_CHQ_NO_LEAVES, "R%3"));
      _l(74);
      set(this._Y_CHEQUE_DESCRIPTION, "");
      _l(77);
      switch (LOCATE(this._Y_CHEQUE_TYPE, this._Y_CHQ_TYPE_LIST, this._POS, 1)) {
        case 0:
          _l(79, 180);
          this._Sys_PostGlobus = lbl_FIND_CROSS_CHECK_ENDO();
          if (this._Sys_PostGlobus != -1)
            return this._Sys_PostGlobus; 
          _l(80, 180);
          this._Sys_PostGlobus = lbl_GET_CHEQUE_DESCRIPTION();
          if (this._Sys_PostGlobus != -1)
            return this._Sys_PostGlobus; 
          _l(82, 180);
          this._Sys_PostGlobus = lbl_UPDATE_HEADER_DET();
          if (this._Sys_PostGlobus != -1)
            return this._Sys_PostGlobus; 
          _l(84);
          if (boolVal(op_equal(this._Y_FIRST_LINE_FLAG, ""))) {
            _l(85);
            set(this._Y_FIRST_LINE_FLAG, 1L);
          } 
          _l(86);
          _l(88);
          do {
            this._isContinue_ = false;
            _l(89);
            if (this._isBreak_ || !boolVal(op_le(this._CI_CNT, this._CI_NO)))
              break; 
            _l(90);
            set(this._CI_ID, get(this._R_CIT, Integer.valueOf(1), this._CI_CNT, 0));
            _l(91);
            set(this._R_CI, "");
            set(this._CI_ERR, "");
            _l(92);
            if (this.session.isUnitTest()) {
              this.session.findStub(new String[] { "F.READ" }).invoke(new Object[] { this._FN_CHEQUE_ISSUE, this._CI_ID, this._R_CI, this._F_CHEQUE_ISSUE, this._CI_ERR });
            } else {
              F_READ_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHEQUE_ISSUE, this._CI_ID, this._R_CI, this._F_CHEQUE_ISSUE, this._CI_ERR });
            } 
            if (this.session.getStateSubroutineAfterCall() == -3)
              return -3; 
            _l(93, 180);
            this._Sys_PostGlobus = lbl_GET_CHEQUE_ISSUE_DETAIL();
            if (this._Sys_PostGlobus != -1)
              return this._Sys_PostGlobus; 
            _l(94);
            if (boolVal(op_equal(get(this._R_CI, 1L, 0, 0), "50"))) {
              _l(95, 180);
              this._Sys_PostGlobus = lbl_GET_ACCOUNT_RIB();
              if (this._Sys_PostGlobus != -1)
                return this._Sys_PostGlobus; 
              _l(96, 180);
              this._Sys_PostGlobus = lbl_GET_COMPANY_NAME();
              if (this._Sys_PostGlobus != -1)
                return this._Sys_PostGlobus; 
              _l(103);
              set(this._N_ORDER_SEQ_NO, op_add(this._N_ORDER_SEQ_NO, 1L));
              _l(104);
              set(this._N_ORDER_SEQ_NO, FMT(this._N_ORDER_SEQ_NO, "R%5"));
              _l(105, 180);
              this._Sys_PostGlobus = lbl_UPDATE_DATA_FILE();
              if (this._Sys_PostGlobus != -1)
                return this._Sys_PostGlobus; 
              _l(107, 180);
              this._Sys_PostGlobus = lbl_UPDATE_CHEQUE_ISSUE();
              if (this._Sys_PostGlobus != -1)
                return this._Sys_PostGlobus; 
            } 
            _l(112);
            _l(114);
            set(this._CI_CNT, op_add(this._CI_CNT, 1L));
            _l(116);
          } while (!this._isBreak_ && this._loop_);
          this._loop_ = true;
          this._isBreak_ = false;
          this._isContinue_ = false;
          break;
      } 
      _l(117);
      _l(126);
      switch (LOCATE(this._Y_CHEQUE_TYPE, this._Y_CHQ_TYPE_LIST, this._POS, 1)) {
        case 0:
          _l(127, 180);
          this._Sys_PostGlobus = lbl_UPDATE_CONCAT_HISTORY();
          if (this._Sys_PostGlobus != -1)
            return this._Sys_PostGlobus; 
          break;
      } 
      _l(128);
      _l(130);
      set(this._N_CHQ_IN, op_add(this._N_CHQ_IN, 1L));
      _l(131);
    } while (!this._isBreak_ && this._loop_);
    this._loop_ = true;
    this._isBreak_ = false;
    this._isContinue_ = false;
    _l(134);
    if (boolVal(op_and(op_ne(this._R_LOCK, ""), op_ne(this._Y_SEQ_NO, this._Y_SEQ_NO_OLD)))) {
      _l(135);
      set(this._Y_LOCK_ID, "CHQ.SEQ.LOCK.BBG.SN");
      _l(136);
      set(this._R_LOCK, Integer.valueOf(1), Integer.valueOf(0), Integer.valueOf(0), this._Y_SEQ_NO);
      _l(137);
      if (this.session.isUnitTest()) {
        this.session.findStub(new String[] { "F.WRITE" }).invoke(new Object[] { this._FN_LOCKING, this._Y_LOCK_ID, this._R_LOCK });
      } else {
        F_WRITE_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_LOCKING, this._Y_LOCK_ID, this._R_LOCK });
      } 
      if (this.session.getStateSubroutineAfterCall() == -3)
        return -3; 
    } 
    _l(138);
    _l(140);
    if (boolVal(op_ne(this._N_R_FINAL_ARRAY, ""))) {
      _l(141);
      set(this._FILE_ID, op_cat("demchqbg", fGet(this._TODAY, Integer.valueOf(5), Integer.valueOf(2))).concat(fGet(this._TODAY, Integer.valueOf(7), Integer.valueOf(8))).concat(".sdf"));
      _l(142);
      if (this.session.isUnitTest()) {
        this.session.findStub(new String[] { "F.WRITE" }).invoke(new Object[] { this._F_FILE_OUT_DIR, this._FILE_ID, this._N_R_FINAL_ARRAY });
      } else {
        F_WRITE_cl.INSTANCE(this.session).invoke(new Object[] { this._F_FILE_OUT_DIR, this._FILE_ID, this._N_R_FINAL_ARRAY });
      } 
      if (this.session.getStateSubroutineAfterCall() == -3)
        return -3; 
    } 
    _l(143);
    _l(145, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_FIND_CROSS_CHECK_ENDO() {
    _l(149);
    set(this._N_CROSS_CHQ, "");
    _l(150);
    set(this._N_Endossable, "");
    _l(152);
    if (boolVal(op_or(op_or(op_or(op_equal(this._Y_CHEQUE_TYPE, "EBCI"), op_equal(this._Y_CHEQUE_TYPE, "EBSC")), op_equal(this._Y_CHEQUE_TYPE, "PBAC")), op_equal(this._Y_CHEQUE_TYPE, "PBSC")))) {
      _l(153);
      set(this._N_CROSS_CHQ, 1L);
      _l(154);
      set(this._N_Endossable, 2L);
    } 
    _l(155);
    _l(156);
    if (boolVal(op_or(op_or(op_or(op_equal(this._Y_CHEQUE_TYPE, "ENCI"), op_equal(this._Y_CHEQUE_TYPE, "ENSC")), op_equal(this._Y_CHEQUE_TYPE, "PNBC")), op_equal(this._Y_CHEQUE_TYPE, "PNSC")))) {
      _l(157);
      set(this._N_CROSS_CHQ, 0L);
      _l(158);
      set(this._N_Endossable, 1L);
    } 
    _l(159);
    _l(161, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_UPDATE_DATA_FILE() {
    _l(177);
    set(this._N_R_LINE_DETAIL, "");
    _l(179);
    set(this._N_R_LINE_DETAIL, op_cat(this._N_FILE_D_REC_ID, this._N_ORDER_SEQ_NO).concat(FMT(this._Y_SEQ_NO, "R#8")).concat(this._N_Country_Code).concat(this._N_branch_Code).concat(this._CODE_GUICHET).concat(this._CLE_RIB).concat(FMT(this._AC_ID, "R#14")));
    _l(180);
    set(this._N_R_LINE_DETAIL, op_cat(this._N_R_LINE_DETAIL, op_cat(FMT(this._N_EMPTY, "L#1"), this._N_BBG_GROUP).concat(this._COMP_ADDR1).concat(this._COMP_ADDR2).concat(this._COMP_ADDR3).concat(this._COMP_ADDR4).concat(FMT(this._N_EMPTY, "R#75")).concat("CI131").concat(FMT(this._N_EMPTY, "R#1")).concat(this._CODE_GUICHET)));
    _l(181);
    set(this._N_R_LINE_DETAIL, op_cat(this._N_R_LINE_DETAIL, op_cat(FMT(this._AC_ID, "R#13"), this._CLE_RIB_A).concat(FMT(this._N_EMPTY, "R#8")).concat(this._NAME_ON_CHQ).concat(this._CHQ_ADDR1).concat(this._CHQ_ADDR2).concat(this._CHQ_TELNO)));
    _l(183);
    set(this._N_R_FINAL_ARRAY, Integer.valueOf(-1), Integer.valueOf(0), Integer.valueOf(0), this._N_R_LINE_DETAIL);
    _l(186, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_UPDATE_CHEQUE_ISSUE() {
    _l(190, 180);
    this._Sys_PostGlobus = lbl_POST_OFS_BULK();
    if (this._Sys_PostGlobus != -1)
      return this._Sys_PostGlobus; 
    _l(191);
    set(this._Y_SEQ_NO, op_add(this._Y_SEQ_NO, get(this._R_CI, Integer.valueOf(30), this._CI_NO_LEAVES_POS, 0)));
    _l(192);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OCOMO" }).invoke(new Object[] { op_cat(" Write Next Sequence CHQ ID  ", this._Y_SEQ_NO) });
    } else {
      OCOMO_cl.INSTANCE(this.session).invoke(new Object[] { op_cat(" Write Next Sequence CHQ ID  ", this._Y_SEQ_NO) });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(194, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_UPDATE_CONCAT_HISTORY() {
    _l(199);
    set(this._CIH_ID, op_cat(this._CIT_ID, ".").concat(this._TODAY));
    _l(200);
    set(this._R_CIH, "");
    set(this._CIH_ERR, "");
    _l(201);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "F.READ" }).invoke(new Object[] { this._FN_CHQ_ISSUE_HIS_BBG_SN, this._CIH_ID, this._R_CIH, this._F_CHQ_ISSUE_HIS_BBG_SN, this._CIH_ERR });
    } else {
      F_READ_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_ISSUE_HIS_BBG_SN, this._CIH_ID, this._R_CIH, this._F_CHQ_ISSUE_HIS_BBG_SN, this._CIH_ERR });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(202);
    set(this._R_CIH, Integer.valueOf(1), Integer.valueOf(-1), Integer.valueOf(0), this._R_CIT);
    _l(203);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "F.WRITE" }).invoke(new Object[] { this._FN_CHQ_ISSUE_HIS_BBG_SN, this._CIH_ID, this._R_CIH });
    } else {
      F_WRITE_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_ISSUE_HIS_BBG_SN, this._CIH_ID, this._R_CIH });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(204);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "F.DELETE" }).invoke(new Object[] { this._FN_CHQ_ISSUE_TODAY_BBG_SN, this._CIT_ID });
    } else {
      F_DELETE_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_ISSUE_TODAY_BBG_SN, this._CIT_ID });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(207, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_GET_CHQ_SEQUENCE_NUMBER() {
    _l(231);
    set(this._Y_LOCK_ID, "CHQ.SEQ.LOCK.BBG.SN");
    _l(232);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "F.READ" }).invoke(new Object[] { this._FN_LOCKING, this._Y_LOCK_ID, this._R_LOCK, this._F_LOCKING_CHQ, this._ERR_LOCKING });
    } else {
      F_READ_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_LOCKING, this._Y_LOCK_ID, this._R_LOCK, this._F_LOCKING_CHQ, this._ERR_LOCKING });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(233);
    if (boolVal(op_equal(get(this._R_LOCK, 1L, 0, 0), ""))) {
      _l(234);
      set(this._Y_SEQ_NO, "1");
    } else {
      _l(235);
      _l(236);
      set(this._Y_SEQ_NO, get(this._R_LOCK, 1L, 0, 0));
    } 
    _l(237);
    _l(238);
    set(this._Y_SEQ_NO, FMT(this._Y_SEQ_NO, "R%7"));
    _l(239);
    set(this._Y_SEQ_NO_OLD, this._Y_SEQ_NO);
    _l(240);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OCOMO" }).invoke(new Object[] { op_cat(" Get Current Sequence CHQ ID  ", this._Y_SEQ_NO) });
    } else {
      OCOMO_cl.INSTANCE(this.session).invoke(new Object[] { op_cat(" Get Current Sequence CHQ ID  ", this._Y_SEQ_NO) });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(242, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_GET_CHEQUE_ISSUE_DETAIL() {
    _l(246);
    set(this._NAME_ON_CHQ, FMT(get(this._R_CI, Integer.valueOf(30), this._CI_NAME_POS, 0), "L#35"));
    _l(247);
    set(this._CHQ_ADDR1, FMT(get(this._R_CI, Integer.valueOf(30), this._CI_ADD1_POS, 0), "L#35"));
    _l(248);
    set(this._CHQ_ADDR2, FMT(get(this._R_CI, Integer.valueOf(30), this._CI_ADD2_POS, 0), "L#35"));
    _l(249);
    set(this._CHQ_ADDR3, FMT(get(this._R_CI, Integer.valueOf(30), this._CI_ADD3_POS, 0), "L#35"));
    _l(250);
    set(this._CHQ_TELNO, FMT(get(this._R_CI, Integer.valueOf(30), this._CI_TEL_NO_POS, 0), "L#35"));
    _l(251);
    set(this._CI_QUANTITY, FMT(get(this._R_CI, Integer.valueOf(30), this._CI_NO_LEAVES_POS, 0), "R#3"));
    _l(253, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_POST_OFS_BULK() {
    _l(258);
    set(this._OFS_REC, op_cat(this._Y_CI_OFS_VERSION, "/I/PROCESS,//").concat(get(this._R_CI, 38L, 0, 0)).concat(",").concat(this._CI_ID).concat(",CHEQUE.STATUS=").concat(this._Y_CQ_PROCESSED_STATUS));
    _l(259);
    this._OFS_REC.concat(",LOCAL.REF:").concat(this._CI_CQ_START_POS).concat(":1=").concat(this._Y_SEQ_NO);
    _l(261);
    set(this._REQ_COMMITTED, "");
    _l(263);
    set(this._OFS_MSG_ID, "");
    _l(264);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OFS.POST.MESSAGE" }).invoke(new Object[] { this._OFS_REC, this._OFS_MSG_ID, this._Y_OFS_SOURCE_ID, this._REQ_COMMITTED });
    } else {
      OFS_POST_MESSAGE_cl.INSTANCE(this.session).invoke(new Object[] { this._OFS_REC, this._OFS_MSG_ID, this._Y_OFS_SOURCE_ID, this._REQ_COMMITTED });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(267, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_GET_ACCOUNT_RIB() {
    _l(304);
    set(this._AC_ID, FIELD(this._CI_ID, ".", Integer.valueOf(2)));
    _l(305);
    set(this._R_ACCOUNT, "");
    set(this._AC_ERR, "");
    _l(306);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "F.READ" }).invoke(new Object[] { this._FN_ACCOUNT, this._AC_ID, this._R_ACCOUNT, this._F_ACCOUNT, this._AC_ERR });
    } else {
      F_READ_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_ACCOUNT, this._AC_ID, this._R_ACCOUNT, this._F_ACCOUNT, this._AC_ERR });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(307);
    set(this._RIB_NUM, "");
    _l(308);
    switch (LOCATE("RIB", this._R_ACCOUNT, Integer.valueOf(99), Integer.valueOf(1), Integer.valueOf(0), null, null, this._AC_TYPE_POS, 1)) {
      case 0:
        _l(309);
        set(this._RIB_NUM, get(this._R_ACCOUNT, Integer.valueOf(100), this._AC_TYPE_POS, 0));
        break;
    } 
    _l(311);
    _l(313);
    set(this._CODE_GUICHET, fGet(this._RIB_NUM, Integer.valueOf(6), Integer.valueOf(5)));
    _l(314);
    set(this._CODE_GUICHET, FMT(this._CODE_GUICHET, "R%5"));
    _l(315);
    set(this._CLE_RIB, fGet(this._RIB_NUM, Integer.valueOf(23), Integer.valueOf(2)));
    _l(317);
    set(this._CLE_RIB_A, FMT(this._CLE_RIB, "R#3"));
    _l(318);
    set(this._CLE_RIB, FMT(this._CLE_RIB, "R#2"));
    _l(320);
    set(this._FMT_RIB, FMT(this._RIB_NUM, "L##### ##### ############ ##"));
    _l(322, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_GET_COMPANY_NAME() {
    _l(326);
    set(this._COMP_ID, get(this._R_ACCOUNT, 252L, 0, 0));
    _l(327);
    set(this._R_COM, "");
    set(this._COM_ERR, "");
    _l(328);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "F.READ" }).invoke(new Object[] { this._FN_COMPANY, this._COMP_ID, this._R_COM, this._F_COMPANY, this._COM_ERR });
    } else {
      F_READ_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_COMPANY, this._COMP_ID, this._R_COM, this._F_COMPANY, this._COM_ERR });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(329);
    set(this._COMP_NAME, get(this._R_COM, 1L, 1, 0));
    _l(330);
    set(this._COMP_NAME, FMT(this._COMP_NAME, "L#25"));
    _l(331);
    set(this._COMP_ADDR, get(this._R_COM, 2L, 0, 0));
    _l(333);
    set(this._COMP_ADDR1, FMT(get(this._COMP_ADDR, 1L, 1, 0), "L#25"));
    _l(334);
    set(this._COMP_ADDR2, FMT(get(this._COMP_ADDR, 1L, 2, 0), "L#25"));
    _l(335);
    set(this._COMP_ADDR3, FMT(get(this._COMP_ADDR, 1L, 3, 0), "L#25"));
    _l(336);
    set(this._COMP_ADDR4, FMT(get(this._COMP_ADDR, 1L, 4, 0), "L#25"));
    _l(338, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_INITIALISE() {
    _l(342);
    set(this._N_R_FINAL_ARRAY, "");
    _l(343);
    set(this._N_R_LINE_DETAIL, "");
    _l(344);
    set(this._N_FILE_H_BEGIN_ID, "9");
    _l(345);
    set(this._N_FILE_H_REC_ID, "1");
    _l(346);
    set(this._N_FILE_D_REC_ID, "2");
    _l(347);
    set(this._N_Country_Code, "39");
    _l(348);
    set(this._N_Country_Code, FMT(this._N_Country_Code, "R#3"));
    _l(349);
    set(this._N_branch_Code, "131");
    _l(351);
    set(this._N_EMPTY, "");
    _l(353);
    this._PROCESS_GO = 1L;
    _l(355);
    set(this._APP_LIST, "CHEQUE.ISSUE");
    _l(356);
    set(this._FLD_LIST, op_cat("L.PRINT.COMP", Character.valueOf(jAtVariable.VM)).concat("L.PRINT.COMP").concat(jAtVariable.VM).concat("L.CHQ.NO.START").concat(jAtVariable.VM).concat("L.NUM.ISSUED").concat(jAtVariable.VM).concat("L.NAME.ON.CHQ"));
    _l(357);
    set(this._FLD_LIST, Integer.valueOf(1), Integer.valueOf(-1), Integer.valueOf(0), op_cat("L.CROSSING", Character.valueOf(jAtVariable.VM)).concat("L.COLL.CENTRE").concat(jAtVariable.VM).concat("L.ACCOUNT.NO").concat(jAtVariable.VM).concat("L.ADDR1.ON.CHQ").concat(jAtVariable.VM).concat("L.ADDR2.ON.CHQ"));
    _l(358);
    set(this._FLD_LIST, Integer.valueOf(1), Integer.valueOf(-1), Integer.valueOf(0), "L.CHQ.PHONE");
    _l(360);
    set(this._FLD_POS, "");
    _l(361);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "MULTI.GET.LOC.REF" }).invoke(new Object[] { this._APP_LIST, this._FLD_LIST, this._FLD_POS });
    } else {
      MULTI_GET_LOC_REF_cl.INSTANCE(this.session).invoke(new Object[] { this._APP_LIST, this._FLD_LIST, this._FLD_POS });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(363);
    set(this._CI_PID_POS, get(this._FLD_POS, 1L, 1, 0));
    _l(364);
    set(this._CI_PRINT_POS, get(this._FLD_POS, 1L, 2, 0));
    _l(365);
    set(this._CI_CQ_START_POS, get(this._FLD_POS, 1L, 3, 0));
    _l(366);
    set(this._CI_NO_LEAVES_POS, get(this._FLD_POS, 1L, 4, 0));
    _l(367);
    set(this._CI_NAME_POS, get(this._FLD_POS, 1L, 5, 0));
    _l(368);
    set(this._CI_CROSSING_POS, get(this._FLD_POS, 1L, 6, 0));
    _l(369);
    set(this._CI_COLL_CTR_POS, get(this._FLD_POS, 1L, 7, 0));
    _l(370);
    set(this._CI_ACCT_POS, get(this._FLD_POS, 1L, 8, 0));
    _l(371);
    set(this._CI_ADD1_POS, get(this._FLD_POS, 1L, 9, 0));
    _l(372);
    set(this._CI_ADD2_POS, get(this._FLD_POS, 1L, 10, 0));
    _l(373);
    set(this._CI_TEL_NO_POS, get(this._FLD_POS, 1L, 11, 0));
    _l(374);
    set(this._Y_COM_MNEMONIC, aGet(this._R_COMPANY, new Object[] { Integer.valueOf(3) }));
    _l(376);
    set(this._N_ORDER_DATE, op_cat(fGet(this._TODAY, Integer.valueOf(7), Integer.valueOf(2)), fGet(this._TODAY, Integer.valueOf(5), Integer.valueOf(2))).concat(fGet(this._TODAY, Integer.valueOf(3), Integer.valueOf(2))));
    _l(377);
    set(this._N_ORDER_DATE, FMT(this._N_ORDER_DATE, "R%6"));
    _l(379);
    set(this._N_BANK_CODE, "0068");
    _l(380);
    set(this._N_BANK_CODE, FMT(this._N_BANK_CODE, "R#9"));
    _l(381);
    set(this._N_VAL_DEF, "1");
    _l(382);
    set(this._N_Endossable, "");
    _l(383);
    set(this._N_vignettes, "vignettes");
    _l(384);
    set(this._N_vignettes, FMT(this._N_vignettes, "R#10"));
    _l(385);
    set(this._N_MODEL_CODE, "013037103127001000009091000081000000010008");
    _l(386);
    set(this._N_MODEL_CODE, FMT(this._N_MODEL_CODE, "R#43"));
    _l(387);
    set(this._N_BBG_GROUP_CI, "BRIDGE BANK GROUP CI");
    _l(388);
    set(this._N_BBG_GROUP_CI, FMT(this._N_BBG_GROUP_CI, "L#20"));
    _l(389);
    set(this._N_BBG_GROUP, "BRIDGE BANK GROUP");
    _l(390);
    set(this._N_BBG_GROUP, FMT(this._N_BBG_GROUP, "L#25"));
    _l(391);
    set(this._N_BARRE_NON, "BARRE NON ENDOSSANS TAL.");
    _l(392);
    set(this._N_BARRE_NON, FMT(this._N_BARRE_NON, "L#28"));
    _l(393);
    set(this._N_BARRE_NO_EXT, fGet(this._N_BARRE_NON, Integer.valueOf(1), Integer.valueOf(15)));
    _l(395, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_OPENFILES() {
    _l(400);
    set(this._FN_CHQ_INTERFACE_EXTRACT_BBG_SN, "F.EB.INTERFACE.EXTRACT.BBG.SN");
    _l(401);
    set(this._F_CHQ_INTERFACE_EXTRACT_BBG_SN, "");
    _l(402);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_CHQ_INTERFACE_EXTRACT_BBG_SN, this._F_CHQ_INTERFACE_EXTRACT_BBG_SN });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_INTERFACE_EXTRACT_BBG_SN, this._F_CHQ_INTERFACE_EXTRACT_BBG_SN });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(406);
    set(this._FN_CHQ_ACCT_SERIAL_NO_BBG_SN, "F.CHQ.ACCT.SERIAL.NO.BBG.SN");
    _l(407);
    set(this._F_CHQ_ACCT_SERIAL_NO_BBG_SN, "");
    _l(408);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_CHQ_ACCT_SERIAL_NO_BBG_SN, this._F_CHQ_ACCT_SERIAL_NO_BBG_SN });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_ACCT_SERIAL_NO_BBG_SN, this._F_CHQ_ACCT_SERIAL_NO_BBG_SN });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(410);
    set(this._FN_CHQ_ISSUE_TODAY_BBG_SN, "F.EB.CHQ.ISSUE.TODAY.BBG.SN");
    _l(411);
    set(this._F_CHQ_ISSUE_TODAY_BBG_SN, "");
    _l(412);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_CHQ_ISSUE_TODAY_BBG_SN, this._F_CHQ_ISSUE_TODAY_BBG_SN });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_ISSUE_TODAY_BBG_SN, this._F_CHQ_ISSUE_TODAY_BBG_SN });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(414);
    set(this._FN_CHQ_ISSUE_HIS_BBG_SN, "F.CHQ.ISSUE.HIS.BBG.SN");
    _l(415);
    set(this._F_CHQ_ISSUE_HIS_BBG_SN, "");
    _l(416);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_CHQ_ISSUE_HIS_BBG_SN, this._F_CHQ_ISSUE_HIS_BBG_SN });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_ISSUE_HIS_BBG_SN, this._F_CHQ_ISSUE_HIS_BBG_SN });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(418);
    set(this._FN_CHQ_ISSUE_SEQ_BBG_SN, "F.CHQ.ISSUE.SEQ.BBG.SN");
    _l(419);
    set(this._F_CHQ_ISSUE_SEQ_BBG_SN, "");
    _l(420);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_CHQ_ISSUE_SEQ_BBG_SN, this._F_CHQ_ISSUE_SEQ_BBG_SN });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_ISSUE_SEQ_BBG_SN, this._F_CHQ_ISSUE_SEQ_BBG_SN });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(422);
    set(this._FN_CHEQUE_ISSUE, "F.CHEQUE.ISSUE");
    _l(423);
    set(this._F_CHEQUE_ISSUE, "");
    _l(424);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_CHEQUE_ISSUE, this._F_CHEQUE_ISSUE });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHEQUE_ISSUE, this._F_CHEQUE_ISSUE });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(426);
    set(this._FN_CHQ_DATA_FILE_BBG_SN, "F.CHQ.DATA.FILE.BBG.SN");
    _l(427);
    set(this._F_CHQ_DATA_FILE_BBG_SN, "");
    _l(428);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_CHQ_DATA_FILE_BBG_SN, this._F_CHQ_DATA_FILE_BBG_SN });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_DATA_FILE_BBG_SN, this._F_CHQ_DATA_FILE_BBG_SN });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(430);
    set(this._FN_CHQ_ISSUE_FAILED_BBG_SN, "F.CHQ.ISSUE.FAILED.BBG.SN");
    _l(431);
    set(this._F_CHQ_ISSUE_FAILED_BBG_SN, "");
    _l(432);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_CHQ_ISSUE_FAILED_BBG_SN, this._F_CHQ_ISSUE_FAILED_BBG_SN });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_ISSUE_FAILED_BBG_SN, this._F_CHQ_ISSUE_FAILED_BBG_SN });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(434);
    set(this._FN_ACCOUNT, "F.ACCOUNT");
    _l(435);
    set(this._F_ACCOUNT, "");
    _l(436);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_ACCOUNT, this._F_ACCOUNT });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_ACCOUNT, this._F_ACCOUNT });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(438);
    set(this._FN_COMPANY, "F.COMPANY");
    _l(439);
    set(this._F_COMPANY, "");
    _l(440);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_COMPANY, this._F_COMPANY });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_COMPANY, this._F_COMPANY });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(443);
    set(this._FN_LOCKING, "F.LOCKING");
    _l(444);
    set(this._F_LOCKING_CHQ, "");
    _l(445);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "OPF" }).invoke(new Object[] { this._FN_LOCKING, this._F_LOCKING_CHQ });
    } else {
      OPF_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_LOCKING, this._F_LOCKING_CHQ });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(446, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_GET_PARAM_VALUES() {
    _l(451);
    set(this._Y_PARAM_IDS, "CQ");
    _l(452);
    set(this._Y_PARAM_NAMES, op_cat("CQ.PROCESS.STATUS", Character.valueOf(jAtVariable.VM)).concat("CQ.PROCESSED.STATUS").concat(jAtVariable.VM).concat("LETTRE.CHEQUE.TYPES").concat(jAtVariable.VM).concat("CQ.BOOK.OUT.PATH").concat(jAtVariable.VM).concat("CQ.BOOK.FILE.PREFIX").concat(jAtVariable.VM).concat("CHQ.UPDATE.VERSION"));
    _l(453);
    set(this._Y_PARAM_NAMES, "CHEQUE.TYPE");
    _l(454);
    set(this._Y_RETURN_CODE, "");
    _l(455);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "U.GET.PARAM.VALUE.BBG" }).invoke(new Object[] { this._Y_PARAM_IDS, this._Y_PARAM_NAMES, this._Y_PARAM_VALUES, this._Y_RETURN_CODE });
    } else {
      U_GET_PARAM_VALUE_BBG_cl.INSTANCE(this.session).invoke(new Object[] { this._Y_PARAM_IDS, this._Y_PARAM_NAMES, this._Y_PARAM_VALUES, this._Y_RETURN_CODE });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(463);
    set(this._Y_CHQ_TYPE_LIST, get(this._Y_PARAM_VALUES, 1L, 1, 0));
    _l(465);
    CHANGE_STMT(Character.valueOf(jAtVariable.SM), Character.valueOf(jAtVariable.VM), this._Y_CHQ_TYPE_LIST, null, null, null);
    _l(466);
    CHANGE_STMT(Character.valueOf(jAtVariable.VM), Character.valueOf(jAtVariable.FM), this._Y_CHQ_TYPE_LIST, null, null, null);
    _l(469);
    set(this._Y_CQ_PROCESS_STATUS, "56");
    _l(470);
    set(this._Y_CQ_PROCESSED_STATUS, "60");
    _l(471);
    set(this._Y_LETTRE_CHEQUE_TYPES, op_cat("LCBA", Character.valueOf(jAtVariable.VM)).concat("CLNB"));
    _l(473, 180);
    this._Sys_PostGlobus = lbl_BOOK_OUT_PATH();
    if (this._Sys_PostGlobus != -1)
      return this._Sys_PostGlobus; 
    _l(474);
    set(this._Y_FILE_NAME_PREFIX, "BBG");
    _l(475);
    set(this._Y_OFS_SOURCE_ID, "CHQ.UPDATE.BBG.SN");
    _l(476);
    set(this._Y_CI_OFS_VERSION, "CHEQUE.ISSUE,UPDATE.CHQ.STATUS.BBG.SN");
    _l(485);
    switch (OPENPATH(this._F_FILE_OUT_DIR, this._Y_CQ_BOOK_OUT_PATH, null, 2)) {
      case 1:
        _l(486);
        this._PROCESS_GO = 0L;
        _l(487);
        if (this.session.isUnitTest()) {
          this.session.findStub(new String[] { "OCOMO" }).invoke(new Object[] { op_cat("Unable to open OUT FILE DIRECTORY ", this._Y_CQ_BOOK_OUT_PATH) });
        } else {
          OCOMO_cl.INSTANCE(this.session).invoke(new Object[] { op_cat("Unable to open OUT FILE DIRECTORY ", this._Y_CQ_BOOK_OUT_PATH) });
        } 
        if (this.session.getStateSubroutineAfterCall() == -3)
          return -3; 
        break;
    } 
    _l(488);
    _l(491);
    set(this._Y_CI_INDEX_LOCK_ID, "CHQ.FILE.INDEX.BBG.CI");
    _l(492);
    set(this._Y_CI_INDEX_SEQ_NO, "");
    _l(493);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "F.READ" }).invoke(new Object[] { this._FN_LOCKING, this._Y_CI_INDEX_LOCK_ID, this._R_LOCK_T, this._F_LOCKING_CHQ, this._ERR_LOCKING });
    } else {
      F_READ_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_LOCKING, this._Y_CI_INDEX_LOCK_ID, this._R_LOCK_T, this._F_LOCKING_CHQ, this._ERR_LOCKING });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(494);
    if (boolVal(op_equal(get(this._R_LOCK_T, 1L, 0, 0), ""))) {
      _l(495);
      set(this._Y_CI_INDEX_SEQ_NO, "1");
    } else {
      _l(496);
      _l(497);
      set(this._Y_CI_INDEX_SEQ_NO, get(this._R_LOCK_T, 1L, 0, 0));
      _l(498);
      set(this._Y_CI_INDEX_SEQ_NO, op_add(this._Y_CI_INDEX_SEQ_NO, 1L));
    } 
    _l(499);
    _l(500);
    set(this._R_LOCK_T, Integer.valueOf(1), Integer.valueOf(0), Integer.valueOf(0), this._Y_CI_INDEX_SEQ_NO);
    _l(501);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "F.WRITE" }).invoke(new Object[] { this._FN_LOCKING, this._Y_CI_INDEX_LOCK_ID, this._R_LOCK_T });
    } else {
      F_WRITE_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_LOCKING, this._Y_CI_INDEX_LOCK_ID, this._R_LOCK_T });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(503);
    set(this._Y_CI_INDEX_SEQ_NO, FMT(this._Y_CI_INDEX_SEQ_NO, "R%4"));
    _l(505, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_UPDATE_HEADER_DET() {
    _l(510);
    set(this._N_R_LINE_DETAIL, "");
    _l(513);
    if (boolVal(op_equal(this._Y_FIRST_LINE_FLAG, ""))) {
      _l(514);
      set(this._N_R_LINE_DETAIL, this._N_FILE_H_BEGIN_ID);
    } else {
      _l(515);
      _l(516);
      set(this._N_R_LINE_DETAIL, this._N_FILE_H_REC_ID);
    } 
    _l(517);
    _l(519);
    set(this._N_R_LINE_DETAIL, op_cat(this._N_R_LINE_DETAIL, op_cat(this._N_ORDER_SEQ_NO, this._Y_CI_INDEX_SEQ_NO).concat(this._N_ORDER_DATE).concat(this._N_VAL_DEF).concat(this._N_Endossable).concat(this._N_CROSS_CHQ).concat(this._Y_CHQ_NO_LEAVES_H)));
    _l(521);
    set(this._N_R_LINE_DETAIL, op_cat(this._N_R_LINE_DETAIL, op_cat(this._N_BANK_CODE, this._Y_CHEQUE_DESCRIPTION).concat(this._Y_CHQ_NO_LEAVES_AH).concat(this._N_vignettes).concat(this._N_MODEL_CODE).concat(this._N_CROSS_CHQ)));
    _l(523);
    set(this._N_R_FINAL_ARRAY, Integer.valueOf(-1), Integer.valueOf(0), Integer.valueOf(0), this._N_R_LINE_DETAIL);
    _l(525, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_BOOK_OUT_PATH() {
    _l(530);
    set(this._Y_COM_MNEMONIC, aGet(this._R_COMPANY, new Object[] { Integer.valueOf(3) }));
    _l(533);
    set(this._S_PARAM_ID, "CHEQUE");
    _l(534);
    set(this._S_PARAM_NAMES, "COM.FILE.OUT");
    _l(535);
    set(this._S_PARAM_VALUES, "");
    _l(536);
    set(this._S_RETURN_CODE, "");
    _l(538);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "U.GET.PARAM.VALUE.BBG" }).invoke(new Object[] { this._S_PARAM_ID, this._S_PARAM_NAMES, this._S_PARAM_VALUES, this._S_RETURN_CODE });
    } else {
      U_GET_PARAM_VALUE_BBG_cl.INSTANCE(this.session).invoke(new Object[] { this._S_PARAM_ID, this._S_PARAM_NAMES, this._S_PARAM_VALUES, this._S_RETURN_CODE });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(539);
    if (boolVal(this._S_PARAM_VALUES)) {
      _l(540);
      set(this._Y_EXT_ID_LIST, this._S_PARAM_VALUES);
      _l(541);
      CHANGE_STMT(Character.valueOf(jAtVariable.SM), Character.valueOf(jAtVariable.FM), this._Y_EXT_ID_LIST, null, null, null);
      _l(542);
      CHANGE_STMT("*", Character.valueOf(jAtVariable.FM), this._Y_EXT_ID_LIST, null, null, null);
      _l(544);
      switch (LOCATE(this._Y_COM_MNEMONIC, this._Y_EXT_ID_LIST, this._sPos, 1)) {
        case 0:
          _l(545);
          set(this._Y_EXT_ID, get(this._Y_EXT_ID_LIST, op_add(this._sPos, 1L), 0, 0));
          break;
      } 
      _l(547);
    } 
    _l(548);
    _l(552);
    if (this.session.isUnitTest()) {
      this.session.findStub(new String[] { "F.READ" }).invoke(new Object[] { this._FN_CHQ_INTERFACE_EXTRACT_BBG_SN, this._Y_EXT_ID, this._R_INT_EXT, this._F_CHQ_INTERFACE_EXTRACT_BBG_SN, this._CIS_ERR });
    } else {
      F_READ_cl.INSTANCE(this.session).invoke(new Object[] { this._FN_CHQ_INTERFACE_EXTRACT_BBG_SN, this._Y_EXT_ID, this._R_INT_EXT, this._F_CHQ_INTERFACE_EXTRACT_BBG_SN, this._CIS_ERR });
    } 
    if (this.session.getStateSubroutineAfterCall() == -3)
      return -3; 
    _l(553);
    set(this._Y_CQ_BOOK_OUT_PATH, get(this._R_INT_EXT, 1L, 0, 0));
    _l(555, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  protected int lbl_GET_CHEQUE_DESCRIPTION() {
    _l(558);
    if (boolVal(op_equal(this._Y_CHEQUE_TYPE, "EBCI"))) {
      _l(559);
      _l(560);
      set(this._Y_CHEQUE_DESCRIPTION, "BARRE NON ENDOSSABLE AVETALON");
    } else if (boolVal(op_equal(this._Y_CHEQUE_TYPE, "PBAC"))) {
      _l(561);
      _l(562);
      set(this._Y_CHEQUE_DESCRIPTION, "BARRE NON ENDOSSABLE AVETALON");
    } else if (boolVal(op_equal(this._Y_CHEQUE_TYPE, "EBSC"))) {
      _l(563);
      _l(564);
      set(this._Y_CHEQUE_DESCRIPTION, "BAREE NON ENDOSSABLE SANSTALON");
    } else if (boolVal(op_equal(this._Y_CHEQUE_TYPE, "PBSC"))) {
      _l(565);
      _l(566);
      set(this._Y_CHEQUE_DESCRIPTION, "BAREE NON ENDOSSABLE SANSTALON");
    } else if (boolVal(op_equal(this._Y_CHEQUE_TYPE, "ENCI"))) {
      _l(567);
      _l(568);
      set(this._Y_CHEQUE_DESCRIPTION, "NON BARRE NON ENDOSSABLE AVETALON SANS BAREEMENT");
    } else if (boolVal(op_equal(this._Y_CHEQUE_TYPE, "PNBC"))) {
      _l(569);
      _l(570);
      set(this._Y_CHEQUE_DESCRIPTION, "NON BARRE NON ENDOSSABLE AVETALON SANS BAREEMENT");
    } else if (boolVal(op_equal(this._Y_CHEQUE_TYPE, "ENSC"))) {
      _l(571);
      _l(572);
      set(this._Y_CHEQUE_DESCRIPTION, "NON BAREE NON ENDOSSABLE SANSTALON SANS BAREEMENT");
    } else if (boolVal(op_equal(this._Y_CHEQUE_TYPE, "PNSC"))) {
      _l(573);
      _l(574);
      set(this._Y_CHEQUE_DESCRIPTION, "NON BAREE NON ENDOSSABLE SANSTALON SANS BAREEMENT");
    } 
    _l(575);
    _l(577);
    if (boolVal(op_gt(LEN(this._Y_CHEQUE_DESCRIPTION), 43L))) {
      _l(578);
      set(this._Y_CHEQUE_DESCRIPTION, fGet(this._Y_CHEQUE_DESCRIPTION, Integer.valueOf(1), Integer.valueOf(43)));
    } 
    _l(579);
    _l(580);
    set(this._Y_CHEQUE_DESCRIPTION, FMT(this._Y_CHEQUE_DESCRIPTION, "L#43"));
    _l(581);
    this._Y_CHEQUE_DESCRIPTION.concat(this._N_BBG_GROUP_CI);
    _l(582, 257);
    this._Sys_ReturnTo = -1;
    return -1;
  }
  
  public jVar invoke(Object... paramVarArgs) {
    if (paramVarArgs.length != 0)
      throw new RuntimeException("Wrong number of arguments : B.CEMPA.CQ.ORDER.FILE.BBG.CIV has 0 arguments "); 
    while (true) {
      try {
        return invoke();
      } catch (NeedRestartException needRestartException) {
        invokeRestart("B_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl", false, new jVar[0]);
        setNeedRestart(false);
        create();
      } 
    } 
  }
  
  public static jRunTime INSTANCE(jSession paramjSession) {
    jRunTime jRunTime1 = null;
    jRunTime1 = paramjSession.getRuntimeCache("B_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl");
    if (jRunTime1 == null) {
      jRunTime1 = new B_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl();
      jRunTime1.init(paramjSession);
    } 
    return jRunTime1;
  }
  
  public void stack(B_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl paramB_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl) {
    if (this.session.setRuntimeCache("B_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl", paramB_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl))
      this._NeedInitialise_ = false; 
  }
  
  public jVar invoke() {
    int i = this.session.getPrecision();
    invokeStart("B_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl", false, new jVar[0]);
    JBCCatchableException jBCCatchableException = null;
    int j = 0;
    try {
      j = main();
    } catch (JBCCatchableException jBCCatchableException1) {
      try {
        if (lbl_CATCH__ERROR() == Integer.MIN_VALUE)
          jBCCatchableException = jBCCatchableException1; 
      } catch (JBCCatchableException jBCCatchableException2) {
        jBCCatchableException = jBCCatchableException2;
      } 
    } 
    if (j > 0) {
      CB(j);
    } else {
      check(j);
    } 
    release();
    invokeStop("B_CEMPA_CQ_ORDER_FILE_BBG_CIV_cl", false, new jVar[0]);
    this.session.setPrecision(i);
    stack(this);
    if (jBCCatchableException != null)
      throw jBCCatchableException; 
    return this._Sys_RetRoutine;
  }
  
  public String getBASICName() {
    return "B.CEMPA.CQ.ORDER.FILE.BBG.CIV";
  }
  
  public static String getBASICNameStatic() {
    return "B.CEMPA.CQ.ORDER.FILE.BBG.CIV";
  }
  
  public int getNbLines() {
    return 349;
  }
  
  public static int getNbLinesStatic() {
    return 349;
  }
  
  public String[] getVarList() {
    if (this._varList_ == null) {
      this._varList_ = new String[155];
      this._varList_[0] = "AC.ERR(jVar)";
      this._varList_[1] = "AC.ID(jVar)";
      this._varList_[2] = "AC.TYPE.POS(jVar)";
      this._varList_[3] = "APP.LIST(jVar)";
      this._varList_[4] = "CHQ.ADDR1(jVar)";
      this._varList_[5] = "CHQ.ADDR2(jVar)";
      this._varList_[6] = "CHQ.ADDR3(jVar)";
      this._varList_[7] = "CHQ.TELNO(jVar)";
      this._varList_[8] = "CI.ACCT.POS(jVar)";
      this._varList_[9] = "CI.ADD1.POS(jVar)";
      this._varList_[10] = "CI.ADD2.POS(jVar)";
      this._varList_[11] = "CI.ADD3.POS(jVar)";
      this._varList_[12] = "CI.CNT(jVar)";
      this._varList_[13] = "CI.COLL.CTR.POS(jVar)";
      this._varList_[14] = "CI.CQ.START.POS(jVar)";
      this._varList_[15] = "CI.CROSSING.POS(jVar)";
      this._varList_[16] = "CI.ERR(jVar)";
      this._varList_[17] = "CI.ID(jVar)";
      this._varList_[18] = "CI.NAME.POS(jVar)";
      this._varList_[19] = "CI.NO(jVar)";
      this._varList_[20] = "CI.NO.LEAVES.POS(jVar)";
      this._varList_[21] = "CI.PID.POS(jVar)";
      this._varList_[22] = "CI.PRINT.POS(jVar)";
      this._varList_[23] = "CI.QUANTITY(jVar)";
      this._varList_[24] = "CI.TEL.NO.POS(jVar)";
      this._varList_[25] = "CIH.ERR(jVar)";
      this._varList_[26] = "CIH.ID(jVar)";
      this._varList_[27] = "CIS.ERR(jVar)";
      this._varList_[28] = "CIT.ERR(jVar)";
      this._varList_[29] = "CIT.ID(jVar)";
      this._varList_[30] = "CLE.RIB(jVar)";
      this._varList_[31] = "CLE.RIB.A(jVar)";
      this._varList_[32] = "CODE.GUICHET(jVar)";
      this._varList_[33] = "COM.ERR(jVar)";
      this._varList_[34] = "COMP.ADDR(jVar)";
      this._varList_[35] = "COMP.ADDR1(jVar)";
      this._varList_[36] = "COMP.ADDR2(jVar)";
      this._varList_[37] = "COMP.ADDR3(jVar)";
      this._varList_[38] = "COMP.ADDR4(jVar)";
      this._varList_[39] = "COMP.ID(jVar)";
      this._varList_[40] = "COMP.NAME(jVar)";
      this._varList_[41] = "DATA.FILE.ID(jVar)";
      this._varList_[42] = "ERR.LOCKING(jVar)";
      this._varList_[43] = "F.ACCOUNT(jVar)";
      this._varList_[44] = "F.CHEQUE.ISSUE(jVar)";
      this._varList_[45] = "F.CHQ.ACCT.SERIAL.NO.BBG.SN(jVar)";
      this._varList_[46] = "F.CHQ.DATA.FILE.BBG.SN(jVar)";
      this._varList_[47] = "F.CHQ.INTERFACE.EXTRACT.BBG.SN(jVar)";
      this._varList_[48] = "F.CHQ.ISSUE.FAILED.BBG.SN(jVar)";
      this._varList_[49] = "F.CHQ.ISSUE.HIS.BBG.SN(jVar)";
      this._varList_[50] = "F.CHQ.ISSUE.SEQ.BBG.SN(jVar)";
      this._varList_[51] = "F.CHQ.ISSUE.TODAY.BBG.SN(jVar)";
      this._varList_[52] = "F.COMPANY(jVar)";
      this._varList_[53] = "F.FILE.OUT.DIR(jVar)";
      this._varList_[54] = "F.LOCKING.CHQ(jVar)";
      this._varList_[55] = "FILE.ID(jVar)";
      this._varList_[56] = "FLD.LIST(jVar)";
      this._varList_[57] = "FLD.POS(jVar)";
      this._varList_[58] = "FMT.RIB(jVar)";
      this._varList_[59] = "FN.ACCOUNT(jVar)";
      this._varList_[60] = "FN.CHEQUE.ISSUE(jVar)";
      this._varList_[61] = "FN.CHQ.ACCT.SERIAL.NO.BBG.SN(jVar)";
      this._varList_[62] = "FN.CHQ.DATA.FILE.BBG.SN(jVar)";
      this._varList_[63] = "FN.CHQ.INTERFACE.EXTRACT.BBG.SN(jVar)";
      this._varList_[64] = "FN.CHQ.ISSUE.FAILED.BBG.SN(jVar)";
      this._varList_[65] = "FN.CHQ.ISSUE.HIS.BBG.SN(jVar)";
      this._varList_[66] = "FN.CHQ.ISSUE.SEQ.BBG.SN(jVar)";
      this._varList_[67] = "FN.CHQ.ISSUE.TODAY.BBG.SN(jVar)";
      this._varList_[68] = "FN.COMPANY(jVar)";
      this._varList_[69] = "FN.LOCKING(jVar)";
      this._varList_[70] = "N.BANK.CODE(jVar)";
      this._varList_[71] = "N.BARRE.NO.EXT(jVar)";
      this._varList_[72] = "N.BARRE.NON(jVar)";
      this._varList_[73] = "N.BBG.GROUP(jVar)";
      this._varList_[74] = "N.BBG.GROUP.CI(jVar)";
      this._varList_[75] = "N.CHQ.IN(jVar)";
      this._varList_[76] = "N.CROSS.CHQ(jVar)";
      this._varList_[77] = "N.Country.Code(jVar)";
      this._varList_[78] = "N.EMPTY(jVar)";
      this._varList_[79] = "N.Endossable(jVar)";
      this._varList_[80] = "N.FILE.D.REC.ID(jVar)";
      this._varList_[81] = "N.FILE.H.BEGIN.ID(jVar)";
      this._varList_[82] = "N.FILE.H.REC.ID(jVar)";
      this._varList_[83] = "N.MODEL.CODE(jVar)";
      this._varList_[84] = "N.OLD.CHQ.TYPE(jVar)";
      this._varList_[85] = "N.ORDER.DATE(jVar)";
      this._varList_[86] = "N.ORDER.SEQ.NO(jVar)";
      this._varList_[87] = "N.R.FINAL.ARRAY(jVar)";
      this._varList_[88] = "N.R.LINE.DETAIL(jVar)";
      this._varList_[89] = "N.VAL.DEF(jVar)";
      this._varList_[90] = "N.branch.Code(jVar)";
      this._varList_[91] = "N.vignettes(jVar)";
      this._varList_[92] = "NAME.ON.CHQ(jVar)";
      this._varList_[93] = "NO.OF.RECS(jVar)";
      this._varList_[94] = "OFS.MSG.ID(jVar)";
      this._varList_[95] = "OFS.REC(jVar)";
      this._varList_[96] = "OFS.RESPONSE(jVar)";
      this._varList_[97] = "PASSED.CHAR(jVar)";
      this._varList_[98] = "PASSED.NO(jVar)";
      this._varList_[99] = "POS(jVar)";
      this._varList_[100] = "PRINTER.ID(jVar)";
      this._varList_[101] = "PROCESS.GO(long)";
      this._varList_[102] = "R.ACCOUNT(jVar)";
      this._varList_[103] = "R.CASN(jVar)";
      this._varList_[104] = "R.CI(jVar)";
      this._varList_[105] = "R.CIH(jVar)";
      this._varList_[106] = "R.CIT(jVar)";
      this._varList_[107] = "R.COM(jVar)";
      this._varList_[108] = "R.COMPANY(Common:THE.GLOBUS.COMMON:1)";
      this._varList_[109] = "R.DATA.FILE(jVar)";
      this._varList_[110] = "R.INT.EXT(jVar)";
      this._varList_[111] = "R.LOCK(jVar)";
      this._varList_[112] = "R.LOCK.T(jVar)";
      this._varList_[113] = "REC.LOCK.ERR(jVar)";
      this._varList_[114] = "REQ.COMMITTED(jVar)";
      this._varList_[115] = "RET.CODE(jVar)";
      this._varList_[116] = "RIB.NUM(jVar)";
      this._varList_[117] = "RUNNING.IN.JBASE(Common:GLOBUS1:9)";
      this._varList_[118] = "S.PARAM.ID(jVar)";
      this._varList_[119] = "S.PARAM.NAMES(jVar)";
      this._varList_[120] = "S.PARAM.VALUES(jVar)";
      this._varList_[121] = "S.RETURN.CODE(jVar)";
      this._varList_[122] = "SELECT.CMD(jVar)";
      this._varList_[123] = "SELECT.LIST(jVar)";
      this._varList_[124] = "STR.POS(jVar)";
      this._varList_[125] = "TODAY(Common:THE.GLOBUS.COMMON:87)";
      this._varList_[126] = "Y.CHEQUE.DESCRIPTION(jVar)";
      this._varList_[127] = "Y.CHEQUE.TYPE(jVar)";
      this._varList_[128] = "Y.CHQ.NO.LEAVES(jVar)";
      this._varList_[129] = "Y.CHQ.NO.LEAVES.AH(jVar)";
      this._varList_[130] = "Y.CHQ.NO.LEAVES.H(jVar)";
      this._varList_[131] = "Y.CHQ.TYPE.LIST(jVar)";
      this._varList_[132] = "Y.CI.INDEX.LOCK.ID(jVar)";
      this._varList_[133] = "Y.CI.INDEX.SEQ.NO(jVar)";
      this._varList_[134] = "Y.CI.OFS.VERSION(jVar)";
      this._varList_[135] = "Y.COM.MNEMONIC(jVar)";
      this._varList_[136] = "Y.CQ.BOOK.OUT.PATH(jVar)";
      this._varList_[137] = "Y.CQ.PROCESS.STATUS(jVar)";
      this._varList_[138] = "Y.CQ.PROCESSED.STATUS(jVar)";
      this._varList_[139] = "Y.EXT.ID(jVar)";
      this._varList_[140] = "Y.EXT.ID.LIST(jVar)";
      this._varList_[141] = "Y.FILE.NAME.PREFIX(jVar)";
      this._varList_[142] = "Y.FILE.UPDATED(jVar)";
      this._varList_[143] = "Y.FIRST.LINE.FLAG(jVar)";
      this._varList_[144] = "Y.LETTRE.CHEQUE.TYPES(jVar)";
      this._varList_[145] = "Y.LOCK.ID(jVar)";
      this._varList_[146] = "Y.OFS.SOURCE.ID(jVar)";
      this._varList_[147] = "Y.PARAM.IDS(jVar)";
      this._varList_[148] = "Y.PARAM.NAMES(jVar)";
      this._varList_[149] = "Y.PARAM.VALUES(jVar)";
      this._varList_[150] = "Y.RETURN.CODE(jVar)";
      this._varList_[151] = "Y.SEQ.NO(jVar)";
      this._varList_[152] = "Y.SEQ.NO.OLD(jVar)";
      this._varList_[153] = "Y.SR.NO(jVar)";
      this._varList_[154] = "sPos(jVar)";
    } 
    return this._varList_;
  }
  
  public String getVarValue(String paramString) {
    int i = paramString.lastIndexOf("(");
    String str = "jVar";
    if (i > 0) {
      str = paramString.substring(i + 1, paramString.length() - 1);
      paramString = paramString.substring(0, i);
    } 
    paramString = jSystem.convertNameVar(paramString);
    try {
      Class<?> clazz = getClass();
      Field field = null;
      try {
        field = clazz.getDeclaredField(paramString);
        str = field.getType().getName();
      } catch (NoSuchFieldException noSuchFieldException) {
        clazz = getClass().getSuperclass();
        field = clazz.getDeclaredField(paramString);
        str = field.getType().getName();
      } 
      if (str.equals("long")) {
        long l = ((Long)field.get(this)).longValue();
        return String.valueOf(l);
      } 
      if (str.equals("String"))
        return (String)field.get(this); 
      if (str.equals("unknow"))
        try {
          jVar jVar2 = (jVar)field.get(this);
          return jVar2.toExternalString();
        } catch (Exception exception) {
          try {
            return (String)field.get(this);
          } catch (Exception exception1) {
            try {
              long l = ((Long)field.get(this)).longValue();
              return String.valueOf(l);
            } catch (Exception exception2) {
              return "N/A";
            } 
          } 
        }  
      jVar jVar1 = (jVar)field.get(this);
      return jVar1.toExternalString();
    } catch (Exception exception) {
      return "N/A";
    } 
  }
  
  public String setVarValue(String paramString1, String paramString2) {
    int i = paramString1.lastIndexOf("(");
    String str = "jVar";
    if (i > 0) {
      str = paramString1.substring(i + 1, paramString1.length() - 1);
      paramString1 = paramString1.substring(0, i);
    } 
    paramString1 = jSystem.convertNameVar(paramString1);
    try {
      Class<?> clazz = getClass();
      Field field = null;
      try {
        field = clazz.getDeclaredField(paramString1);
      } catch (NoSuchFieldException noSuchFieldException) {
        clazz = getClass().getSuperclass();
        field = clazz.getDeclaredField(paramString1);
      } 
      if (str.equals("long")) {
        field.setLong(this, Long.parseLong(paramString2));
        return paramString2;
      } 
      if (str.equals("String")) {
        field.set(this, paramString2);
        return paramString2;
      } 
      jVar jVar1 = (jVar)field.get(this);
      jVar1.set(paramString2);
      return paramString2;
    } catch (Exception exception) {
      return "! Failure !";
    } 
  }
  
  public int INSERT__I__COMMON() {
    if (this._R_COMPANY == null) {
      this._R_COMPANY = this.session.getCommonNamed("THE.GLOBUS.COMMON", 1, this._R_COMPANY, "R.COMPANY");
      if (this._R_COMPANY.getNeedCommonInit())
        DIM(this._R_COMPANY, new Object[] { Integer.valueOf(500) }); 
      this._R_COMPANY.setNeedCommonInit(false);
    } 
    if (this._TODAY == null)
      this._TODAY = this.session.getCommonNamed("THE.GLOBUS.COMMON", 87, "TODAY"); 
    if (this._RUNNING_IN_JBASE == null)
      this._RUNNING_IN_JBASE = this.session.getCommonNamed("GLOBUS1", 9, "RUNNING.IN.JBASE"); 
    _l(257, "I_COMMON", false);
    set(this._PASSED_NO, "");
    _l(258, "I_COMMON", false);
    set(this._PASSED_CHAR, "");
    _l(259, "I_COMMON", false);
    _l(260, "I_COMMON", false);
    _l(261, "I_COMMON", false);
    _l(262, "I_COMMON", false);
    _l(264, "I_COMMON", false);
    if (boolVal(this._RUNNING_IN_JBASE)) {
      _l(265, "I_COMMON", false);
      PRECISION(13);
    } else {
      _l(266, "I_COMMON", false);
      _l(267, "I_COMMON", false);
      PRECISION(6);
    } 
    _l(268, "I_COMMON", false);
    return 0;
  }
  
  public int INSERT__I__F_LOCKING() {
    return 0;
  }
  
  public int INSERT__I__F_EB_INTERFACE_EXTRACT_BBG_SN() {
    return 0;
  }
  
  public int INSERT__I__F_COMPANY() {
    return 0;
  }
  
  public int INSERT__I__F_CHEQUE_ISSUE() {
    return 0;
  }
  
  public int INSERT__I__EQUATE() {
    return 0;
  }
  
  public int INSERT__I__F_ACCOUNT() {
    return 0;
  }
  
  public void init(jSession paramjSession) {
    super.init(paramjSession);
    if (this._NeedInitialise_) {
      create();
    } else {
      reset();
    } 
  }
  
  public String[] getComponentList() {
    if (this._componentList_ == null)
      this._componentList_ = new String[0]; 
    return this._componentList_;
  }
  
  public void create() {
    this._PASSED_CHAR = jVarFactory.get();
    this._Y_FIRST_LINE_FLAG = jVarFactory.get();
    this._F_CHQ_ISSUE_HIS_BBG_SN = jVarFactory.get();
    this._F_CHQ_ISSUE_FAILED_BBG_SN = jVarFactory.get();
    this._DATA_FILE_ID = jVarFactory.get();
    this._N_BARRE_NON = jVarFactory.get();
    this._CHQ_ADDR3 = jVarFactory.get();
    this._CI_ADD2_POS = jVarFactory.get();
    this._CHQ_ADDR2 = jVarFactory.get();
    this._CHQ_ADDR1 = jVarFactory.get();
    this._S_PARAM_NAMES = jVarFactory.get();
    this._FILE_ID = jVarFactory.get();
    this._Y_CQ_BOOK_OUT_PATH = jVarFactory.get();
    this._CI_TEL_NO_POS = jVarFactory.get();
    this._OFS_MSG_ID = jVarFactory.get();
    this._Y_LETTRE_CHEQUE_TYPES = jVarFactory.get();
    this._FN_ACCOUNT = jVarFactory.get();
    this._APP_LIST = jVarFactory.get();
    this._CI_NO = jVarFactory.get();
    this._N_FILE_D_REC_ID = jVarFactory.get();
    this._STR_POS = jVarFactory.get();
    this._CLE_RIB_A = jVarFactory.get();
    this._Y_COM_MNEMONIC = jVarFactory.get();
    this._CIT_ID = jVarFactory.get();
    this._CI_ADD1_POS = jVarFactory.get();
    this._Y_PARAM_IDS = jVarFactory.get();
    this._S_PARAM_ID = jVarFactory.get();
    this._Y_SEQ_NO_OLD = jVarFactory.get();
    this._RIB_NUM = jVarFactory.get();
    this._Y_CHQ_NO_LEAVES_AH = jVarFactory.get();
    this._F_CHQ_ISSUE_TODAY_BBG_SN = jVarFactory.get();
    this._AC_TYPE_POS = jVarFactory.get();
    this._PROCESS_GO = 0L;
    this._CI_COLL_CTR_POS = jVarFactory.get();
    this._CI_NAME_POS = jVarFactory.get();
    this._FMT_RIB = jVarFactory.get();
    this._R_ACCOUNT = jVarFactory.get();
    this._N_ORDER_DATE = jVarFactory.get();
    this._N_BANK_CODE = jVarFactory.get();
    this._COM_ERR = jVarFactory.get();
    this._Y_RETURN_CODE = jVarFactory.get();
    this._CIH_ID = jVarFactory.get();
    this._Y_CI_OFS_VERSION = jVarFactory.get();
    this._R_CASN = jVarFactory.get();
    this._F_CHQ_ISSUE_SEQ_BBG_SN = jVarFactory.get();
    this._N_FILE_H_REC_ID = jVarFactory.get();
    this._N_EMPTY = jVarFactory.get();
    this._F_CHEQUE_ISSUE = jVarFactory.get();
    this._CI_QUANTITY = jVarFactory.get();
    this._Y_CHQ_NO_LEAVES_H = jVarFactory.get();
    this._CLE_RIB = jVarFactory.get();
    this._Y_PARAM_NAMES = jVarFactory.get();
    this._Y_CQ_PROCESSED_STATUS = jVarFactory.get();
    this._CI_ID = jVarFactory.get();
    this._COMP_NAME = jVarFactory.get();
    this._Y_EXT_ID = jVarFactory.get();
    this._N_vignettes = jVarFactory.get();
    this._CI_NO_LEAVES_POS = jVarFactory.get();
    this._N_Country_Code = jVarFactory.get();
    this._FN_COMPANY = jVarFactory.get();
    this._CIH_ERR = jVarFactory.get();
    this._F_CHQ_ACCT_SERIAL_NO_BBG_SN = jVarFactory.get();
    this._R_COM = jVarFactory.get();
    this._sPos = jVarFactory.get();
    this._FN_CHQ_ISSUE_HIS_BBG_SN = jVarFactory.get();
    this._N_R_LINE_DETAIL = jVarFactory.get();
    this._CI_PRINT_POS = jVarFactory.get();
    this._Y_FILE_UPDATED = jVarFactory.get();
    this._POS = jVarFactory.get();
    this._Y_CHEQUE_DESCRIPTION = jVarFactory.get();
    this._Y_EXT_ID_LIST = jVarFactory.get();
    this._Y_PARAM_VALUES = jVarFactory.get();
    this._REC_LOCK_ERR = jVarFactory.get();
    this._AC_ID = jVarFactory.get();
    this._N_VAL_DEF = jVarFactory.get();
    this._N_R_FINAL_ARRAY = jVarFactory.get();
    this._R_INT_EXT = jVarFactory.get();
    this._COMP_ADDR4 = jVarFactory.get();
    this._COMP_ADDR3 = jVarFactory.get();
    this._COMP_ADDR2 = jVarFactory.get();
    this._COMP_ADDR1 = jVarFactory.get();
    this._N_ORDER_SEQ_NO = jVarFactory.get();
    this._F_ACCOUNT = jVarFactory.get();
    this._F_CHQ_INTERFACE_EXTRACT_BBG_SN = jVarFactory.get();
    this._FN_CHQ_INTERFACE_EXTRACT_BBG_SN = jVarFactory.get();
    this._CODE_GUICHET = jVarFactory.get();
    this._CI_CROSSING_POS = jVarFactory.get();
    this._FN_CHEQUE_ISSUE = jVarFactory.get();
    this._FN_LOCKING = jVarFactory.get();
    this._CI_CNT = jVarFactory.get();
    this._PASSED_NO = jVarFactory.get();
    this._FN_CHQ_ISSUE_FAILED_BBG_SN = jVarFactory.get();
    this._SELECT_LIST = jVarFactory.get();
    this._N_BBG_GROUP = jVarFactory.get();
    this._FN_CHQ_ACCT_SERIAL_NO_BBG_SN = jVarFactory.get();
    this._CIT_ERR = jVarFactory.get();
    this._F_CHQ_DATA_FILE_BBG_SN = jVarFactory.get();
    this._AC_ERR = jVarFactory.get();
    this._N_CROSS_CHQ = jVarFactory.get();
    this._N_MODEL_CODE = jVarFactory.get();
    this._Y_FILE_NAME_PREFIX = jVarFactory.get();
    this._R_DATA_FILE = jVarFactory.get();
    this._R_CI = jVarFactory.get();
    this._FLD_LIST = jVarFactory.get();
    this._CIS_ERR = jVarFactory.get();
    this._Y_CHQ_NO_LEAVES = jVarFactory.get();
    this._N_BBG_GROUP_CI = jVarFactory.get();
    this._R_CIT = jVarFactory.get();
    this._FN_CHQ_ISSUE_SEQ_BBG_SN = jVarFactory.get();
    this._R_CIH = jVarFactory.get();
    this._FLD_POS = jVarFactory.get();
    this._N_BARRE_NO_EXT = jVarFactory.get();
    this._RET_CODE = jVarFactory.get();
    this._FN_CHQ_ISSUE_TODAY_BBG_SN = jVarFactory.get();
    this._F_LOCKING_CHQ = jVarFactory.get();
    this._N_branch_Code = jVarFactory.get();
    this._R_LOCK_T = jVarFactory.get();
    this._REQ_COMMITTED = jVarFactory.get();
    this._OFS_RESPONSE = jVarFactory.get();
    this._N_FILE_H_BEGIN_ID = jVarFactory.get();
    this._CI_PID_POS = jVarFactory.get();
    this._Y_CI_INDEX_SEQ_NO = jVarFactory.get();
    this._R_LOCK = jVarFactory.get();
    this._COMP_ID = jVarFactory.get();
    this._S_PARAM_VALUES = jVarFactory.get();
    this._N_Endossable = jVarFactory.get();
    this._NAME_ON_CHQ = jVarFactory.get();
    this._F_COMPANY = jVarFactory.get();
    this._N_CHQ_IN = jVarFactory.get();
    this._COMP_ADDR = jVarFactory.get();
    this._Y_CI_INDEX_LOCK_ID = jVarFactory.get();
    this._ERR_LOCKING = jVarFactory.get();
    this._Y_CHEQUE_TYPE = jVarFactory.get();
    this._NO_OF_RECS = jVarFactory.get();
    this._CI_ERR = jVarFactory.get();
    this._Y_LOCK_ID = jVarFactory.get();
    this._N_OLD_CHQ_TYPE = jVarFactory.get();
    this._Y_SEQ_NO = jVarFactory.get();
    this._CI_ACCT_POS = jVarFactory.get();
    this._PRINTER_ID = jVarFactory.get();
    this._Y_OFS_SOURCE_ID = jVarFactory.get();
    this._OFS_REC = jVarFactory.get();
    this._Y_CQ_PROCESS_STATUS = jVarFactory.get();
    this._S_RETURN_CODE = jVarFactory.get();
    this._CI_CQ_START_POS = jVarFactory.get();
    this._CI_ADD3_POS = jVarFactory.get();
    this._Y_SR_NO = jVarFactory.get();
    this._SELECT_CMD = jVarFactory.get();
    this._F_FILE_OUT_DIR = jVarFactory.get();
    this._Y_CHQ_TYPE_LIST = jVarFactory.get();
    this._FN_CHQ_DATA_FILE_BBG_SN = jVarFactory.get();
    this._CHQ_TELNO = jVarFactory.get();
  }
  
  public void reset() {
    this._PASSED_CHAR.reset();
    this._Y_FIRST_LINE_FLAG.reset();
    this._F_CHQ_ISSUE_HIS_BBG_SN.reset();
    this._F_CHQ_ISSUE_FAILED_BBG_SN.reset();
    this._DATA_FILE_ID.reset();
    this._N_BARRE_NON.reset();
    this._CHQ_ADDR3.reset();
    this._CI_ADD2_POS.reset();
    this._CHQ_ADDR2.reset();
    this._CHQ_ADDR1.reset();
    this._S_PARAM_NAMES.reset();
    this._FILE_ID.reset();
    this._Y_CQ_BOOK_OUT_PATH.reset();
    this._CI_TEL_NO_POS.reset();
    this._OFS_MSG_ID.reset();
    this._Y_LETTRE_CHEQUE_TYPES.reset();
    this._FN_ACCOUNT.reset();
    this._APP_LIST.reset();
    this._CI_NO.reset();
    this._N_FILE_D_REC_ID.reset();
    this._STR_POS.reset();
    this._CLE_RIB_A.reset();
    this._Y_COM_MNEMONIC.reset();
    this._CIT_ID.reset();
    this._CI_ADD1_POS.reset();
    this._Y_PARAM_IDS.reset();
    this._S_PARAM_ID.reset();
    this._Y_SEQ_NO_OLD.reset();
    this._RIB_NUM.reset();
    this._Y_CHQ_NO_LEAVES_AH.reset();
    this._F_CHQ_ISSUE_TODAY_BBG_SN.reset();
    this._AC_TYPE_POS.reset();
    this._PROCESS_GO = 0L;
    this._CI_COLL_CTR_POS.reset();
    this._CI_NAME_POS.reset();
    this._FMT_RIB.reset();
    this._R_ACCOUNT.reset();
    this._N_ORDER_DATE.reset();
    this._N_BANK_CODE.reset();
    this._COM_ERR.reset();
    this._Y_RETURN_CODE.reset();
    this._CIH_ID.reset();
    this._Y_CI_OFS_VERSION.reset();
    this._R_CASN.reset();
    this._F_CHQ_ISSUE_SEQ_BBG_SN.reset();
    this._N_FILE_H_REC_ID.reset();
    this._N_EMPTY.reset();
    this._F_CHEQUE_ISSUE.reset();
    this._CI_QUANTITY.reset();
    this._Y_CHQ_NO_LEAVES_H.reset();
    this._CLE_RIB.reset();
    this._Y_PARAM_NAMES.reset();
    this._Y_CQ_PROCESSED_STATUS.reset();
    this._CI_ID.reset();
    this._COMP_NAME.reset();
    this._Y_EXT_ID.reset();
    this._N_vignettes.reset();
    this._CI_NO_LEAVES_POS.reset();
    this._N_Country_Code.reset();
    this._FN_COMPANY.reset();
    this._CIH_ERR.reset();
    this._F_CHQ_ACCT_SERIAL_NO_BBG_SN.reset();
    this._R_COM.reset();
    this._sPos.reset();
    this._FN_CHQ_ISSUE_HIS_BBG_SN.reset();
    this._N_R_LINE_DETAIL.reset();
    this._CI_PRINT_POS.reset();
    this._Y_FILE_UPDATED.reset();
    this._POS.reset();
    this._Y_CHEQUE_DESCRIPTION.reset();
    this._Y_EXT_ID_LIST.reset();
    this._Y_PARAM_VALUES.reset();
    this._REC_LOCK_ERR.reset();
    this._AC_ID.reset();
    this._N_VAL_DEF.reset();
    this._N_R_FINAL_ARRAY.reset();
    this._R_INT_EXT.reset();
    this._COMP_ADDR4.reset();
    this._COMP_ADDR3.reset();
    this._COMP_ADDR2.reset();
    this._COMP_ADDR1.reset();
    this._N_ORDER_SEQ_NO.reset();
    this._F_ACCOUNT.reset();
    this._F_CHQ_INTERFACE_EXTRACT_BBG_SN.reset();
    this._FN_CHQ_INTERFACE_EXTRACT_BBG_SN.reset();
    this._CODE_GUICHET.reset();
    this._CI_CROSSING_POS.reset();
    this._FN_CHEQUE_ISSUE.reset();
    this._FN_LOCKING.reset();
    this._CI_CNT.reset();
    this._PASSED_NO.reset();
    this._FN_CHQ_ISSUE_FAILED_BBG_SN.reset();
    this._SELECT_LIST.reset();
    this._N_BBG_GROUP.reset();
    this._FN_CHQ_ACCT_SERIAL_NO_BBG_SN.reset();
    this._CIT_ERR.reset();
    this._F_CHQ_DATA_FILE_BBG_SN.reset();
    this._AC_ERR.reset();
    this._N_CROSS_CHQ.reset();
    this._N_MODEL_CODE.reset();
    this._Y_FILE_NAME_PREFIX.reset();
    this._R_DATA_FILE.reset();
    this._R_CI.reset();
    this._FLD_LIST.reset();
    this._CIS_ERR.reset();
    this._Y_CHQ_NO_LEAVES.reset();
    this._N_BBG_GROUP_CI.reset();
    this._R_CIT.reset();
    this._FN_CHQ_ISSUE_SEQ_BBG_SN.reset();
    this._R_CIH.reset();
    this._FLD_POS.reset();
    this._N_BARRE_NO_EXT.reset();
    this._RET_CODE.reset();
    this._FN_CHQ_ISSUE_TODAY_BBG_SN.reset();
    this._F_LOCKING_CHQ.reset();
    this._N_branch_Code.reset();
    this._R_LOCK_T.reset();
    this._REQ_COMMITTED.reset();
    this._OFS_RESPONSE.reset();
    this._N_FILE_H_BEGIN_ID.reset();
    this._CI_PID_POS.reset();
    this._Y_CI_INDEX_SEQ_NO.reset();
    this._R_LOCK.reset();
    this._COMP_ID.reset();
    this._S_PARAM_VALUES.reset();
    this._N_Endossable.reset();
    this._NAME_ON_CHQ.reset();
    this._F_COMPANY.reset();
    this._N_CHQ_IN.reset();
    this._COMP_ADDR.reset();
    this._Y_CI_INDEX_LOCK_ID.reset();
    this._ERR_LOCKING.reset();
    this._Y_CHEQUE_TYPE.reset();
    this._NO_OF_RECS.reset();
    this._CI_ERR.reset();
    this._Y_LOCK_ID.reset();
    this._N_OLD_CHQ_TYPE.reset();
    this._Y_SEQ_NO.reset();
    this._CI_ACCT_POS.reset();
    this._PRINTER_ID.reset();
    this._Y_OFS_SOURCE_ID.reset();
    this._OFS_REC.reset();
    this._Y_CQ_PROCESS_STATUS.reset();
    this._S_RETURN_CODE.reset();
    this._CI_CQ_START_POS.reset();
    this._CI_ADD3_POS.reset();
    this._Y_SR_NO.reset();
    this._SELECT_CMD.reset();
    this._F_FILE_OUT_DIR.reset();
    this._Y_CHQ_TYPE_LIST.reset();
    this._FN_CHQ_DATA_FILE_BBG_SN.reset();
    this._CHQ_TELNO.reset();
  }
  
  public void CLEAR() {
    this._file0001.CLEAR();
    this._PASSED_CHAR.CLEAR();
    this._Y_FIRST_LINE_FLAG.CLEAR();
    this._F_CHQ_ISSUE_HIS_BBG_SN.CLEAR();
    this._F_CHQ_ISSUE_FAILED_BBG_SN.CLEAR();
    this._DATA_FILE_ID.CLEAR();
    this._N_BARRE_NON.CLEAR();
    this._CHQ_ADDR3.CLEAR();
    this._CI_ADD2_POS.CLEAR();
    this._CHQ_ADDR2.CLEAR();
    this._CHQ_ADDR1.CLEAR();
    this._S_PARAM_NAMES.CLEAR();
    this._FILE_ID.CLEAR();
    this._Y_CQ_BOOK_OUT_PATH.CLEAR();
    this._CI_TEL_NO_POS.CLEAR();
    this._OFS_MSG_ID.CLEAR();
    this._Y_LETTRE_CHEQUE_TYPES.CLEAR();
    this._FN_ACCOUNT.CLEAR();
    this._APP_LIST.CLEAR();
    this._CI_NO.CLEAR();
    this._N_FILE_D_REC_ID.CLEAR();
    this._STR_POS.CLEAR();
    this._CLE_RIB_A.CLEAR();
    this._Y_COM_MNEMONIC.CLEAR();
    this._CIT_ID.CLEAR();
    this._CI_ADD1_POS.CLEAR();
    this._Y_PARAM_IDS.CLEAR();
    this._S_PARAM_ID.CLEAR();
    this._Y_SEQ_NO_OLD.CLEAR();
    this._RIB_NUM.CLEAR();
    this._Y_CHQ_NO_LEAVES_AH.CLEAR();
    this._F_CHQ_ISSUE_TODAY_BBG_SN.CLEAR();
    this._AC_TYPE_POS.CLEAR();
    this._PROCESS_GO = 0L;
    this._CI_COLL_CTR_POS.CLEAR();
    this._CI_NAME_POS.CLEAR();
    this._FMT_RIB.CLEAR();
    this._R_ACCOUNT.CLEAR();
    this._N_ORDER_DATE.CLEAR();
    this._N_BANK_CODE.CLEAR();
    this._COM_ERR.CLEAR();
    this._Y_RETURN_CODE.CLEAR();
    this._CIH_ID.CLEAR();
    this._Y_CI_OFS_VERSION.CLEAR();
    this._R_CASN.CLEAR();
    this._F_CHQ_ISSUE_SEQ_BBG_SN.CLEAR();
    this._N_FILE_H_REC_ID.CLEAR();
    this._N_EMPTY.CLEAR();
    this._F_CHEQUE_ISSUE.CLEAR();
    this._CI_QUANTITY.CLEAR();
    this._Y_CHQ_NO_LEAVES_H.CLEAR();
    this._CLE_RIB.CLEAR();
    this._Y_PARAM_NAMES.CLEAR();
    this._Y_CQ_PROCESSED_STATUS.CLEAR();
    this._CI_ID.CLEAR();
    this._COMP_NAME.CLEAR();
    this._Y_EXT_ID.CLEAR();
    this._N_vignettes.CLEAR();
    this._CI_NO_LEAVES_POS.CLEAR();
    this._N_Country_Code.CLEAR();
    this._FN_COMPANY.CLEAR();
    this._CIH_ERR.CLEAR();
    this._F_CHQ_ACCT_SERIAL_NO_BBG_SN.CLEAR();
    this._R_COM.CLEAR();
    this._sPos.CLEAR();
    this._FN_CHQ_ISSUE_HIS_BBG_SN.CLEAR();
    this._N_R_LINE_DETAIL.CLEAR();
    this._CI_PRINT_POS.CLEAR();
    this._Y_FILE_UPDATED.CLEAR();
    this._POS.CLEAR();
    this._Y_CHEQUE_DESCRIPTION.CLEAR();
    this._Y_EXT_ID_LIST.CLEAR();
    this._Y_PARAM_VALUES.CLEAR();
    this._REC_LOCK_ERR.CLEAR();
    this._AC_ID.CLEAR();
    this._N_VAL_DEF.CLEAR();
    this._N_R_FINAL_ARRAY.CLEAR();
    this._R_INT_EXT.CLEAR();
    this._COMP_ADDR4.CLEAR();
    this._COMP_ADDR3.CLEAR();
    this._COMP_ADDR2.CLEAR();
    this._COMP_ADDR1.CLEAR();
    this._N_ORDER_SEQ_NO.CLEAR();
    this._F_ACCOUNT.CLEAR();
    this._F_CHQ_INTERFACE_EXTRACT_BBG_SN.CLEAR();
    this._FN_CHQ_INTERFACE_EXTRACT_BBG_SN.CLEAR();
    this._CODE_GUICHET.CLEAR();
    this._CI_CROSSING_POS.CLEAR();
    this._FN_CHEQUE_ISSUE.CLEAR();
    this._FN_LOCKING.CLEAR();
    this._CI_CNT.CLEAR();
    this._PASSED_NO.CLEAR();
    this._FN_CHQ_ISSUE_FAILED_BBG_SN.CLEAR();
    this._SELECT_LIST.CLEAR();
    this._N_BBG_GROUP.CLEAR();
    this._FN_CHQ_ACCT_SERIAL_NO_BBG_SN.CLEAR();
    this._CIT_ERR.CLEAR();
    this._F_CHQ_DATA_FILE_BBG_SN.CLEAR();
    this._AC_ERR.CLEAR();
    this._N_CROSS_CHQ.CLEAR();
    this._N_MODEL_CODE.CLEAR();
    this._Y_FILE_NAME_PREFIX.CLEAR();
    this._R_DATA_FILE.CLEAR();
    this._R_CI.CLEAR();
    this._FLD_LIST.CLEAR();
    this._CIS_ERR.CLEAR();
    this._Y_CHQ_NO_LEAVES.CLEAR();
    this._N_BBG_GROUP_CI.CLEAR();
    this._R_CIT.CLEAR();
    this._FN_CHQ_ISSUE_SEQ_BBG_SN.CLEAR();
    this._R_CIH.CLEAR();
    this._FLD_POS.CLEAR();
    this._N_BARRE_NO_EXT.CLEAR();
    this._RET_CODE.CLEAR();
    this._FN_CHQ_ISSUE_TODAY_BBG_SN.CLEAR();
    this._F_LOCKING_CHQ.CLEAR();
    this._N_branch_Code.CLEAR();
    this._R_LOCK_T.CLEAR();
    this._REQ_COMMITTED.CLEAR();
    this._OFS_RESPONSE.CLEAR();
    this._N_FILE_H_BEGIN_ID.CLEAR();
    this._CI_PID_POS.CLEAR();
    this._Y_CI_INDEX_SEQ_NO.CLEAR();
    this._R_LOCK.CLEAR();
    this._COMP_ID.CLEAR();
    this._S_PARAM_VALUES.CLEAR();
    this._N_Endossable.CLEAR();
    this._NAME_ON_CHQ.CLEAR();
    this._F_COMPANY.CLEAR();
    this._N_CHQ_IN.CLEAR();
    this._COMP_ADDR.CLEAR();
    this._Y_CI_INDEX_LOCK_ID.CLEAR();
    this._ERR_LOCKING.CLEAR();
    this._Y_CHEQUE_TYPE.CLEAR();
    this._NO_OF_RECS.CLEAR();
    this._CI_ERR.CLEAR();
    this._Y_LOCK_ID.CLEAR();
    this._N_OLD_CHQ_TYPE.CLEAR();
    this._Y_SEQ_NO.CLEAR();
    this._CI_ACCT_POS.CLEAR();
    this._PRINTER_ID.CLEAR();
    this._Y_OFS_SOURCE_ID.CLEAR();
    this._OFS_REC.CLEAR();
    this._Y_CQ_PROCESS_STATUS.CLEAR();
    this._S_RETURN_CODE.CLEAR();
    this._CI_CQ_START_POS.CLEAR();
    this._CI_ADD3_POS.CLEAR();
    this._Y_SR_NO.CLEAR();
    this._SELECT_CMD.CLEAR();
    this._F_FILE_OUT_DIR.CLEAR();
    this._Y_CHQ_TYPE_LIST.CLEAR();
    this._FN_CHQ_DATA_FILE_BBG_SN.CLEAR();
    this._CHQ_TELNO.CLEAR();
  }
  
  public void release() {
    this._PASSED_CHAR.release();
    this._Y_FIRST_LINE_FLAG.release();
    this._F_CHQ_ISSUE_HIS_BBG_SN.release();
    this._F_CHQ_ISSUE_FAILED_BBG_SN.release();
    this._DATA_FILE_ID.release();
    this._N_BARRE_NON.release();
    this._CHQ_ADDR3.release();
    this._CI_ADD2_POS.release();
    this._CHQ_ADDR2.release();
    this._CHQ_ADDR1.release();
    this._S_PARAM_NAMES.release();
    this._FILE_ID.release();
    this._Y_CQ_BOOK_OUT_PATH.release();
    this._CI_TEL_NO_POS.release();
    this._OFS_MSG_ID.release();
    this._Y_LETTRE_CHEQUE_TYPES.release();
    this._FN_ACCOUNT.release();
    this._APP_LIST.release();
    this._CI_NO.release();
    this._N_FILE_D_REC_ID.release();
    this._STR_POS.release();
    this._CLE_RIB_A.release();
    this._Y_COM_MNEMONIC.release();
    this._CIT_ID.release();
    this._CI_ADD1_POS.release();
    this._Y_PARAM_IDS.release();
    this._S_PARAM_ID.release();
    this._Y_SEQ_NO_OLD.release();
    this._RIB_NUM.release();
    this._Y_CHQ_NO_LEAVES_AH.release();
    this._F_CHQ_ISSUE_TODAY_BBG_SN.release();
    this._AC_TYPE_POS.release();
    this._PROCESS_GO = 0L;
    this._CI_COLL_CTR_POS.release();
    this._CI_NAME_POS.release();
    this._FMT_RIB.release();
    this._R_ACCOUNT.release();
    this._N_ORDER_DATE.release();
    this._N_BANK_CODE.release();
    this._COM_ERR.release();
    this._Y_RETURN_CODE.release();
    this._CIH_ID.release();
    this._Y_CI_OFS_VERSION.release();
    this._R_CASN.release();
    this._F_CHQ_ISSUE_SEQ_BBG_SN.release();
    this._N_FILE_H_REC_ID.release();
    this._N_EMPTY.release();
    this._F_CHEQUE_ISSUE.release();
    this._CI_QUANTITY.release();
    this._Y_CHQ_NO_LEAVES_H.release();
    this._CLE_RIB.release();
    this._Y_PARAM_NAMES.release();
    this._Y_CQ_PROCESSED_STATUS.release();
    this._CI_ID.release();
    this._COMP_NAME.release();
    this._Y_EXT_ID.release();
    this._N_vignettes.release();
    this._CI_NO_LEAVES_POS.release();
    this._N_Country_Code.release();
    this._FN_COMPANY.release();
    this._CIH_ERR.release();
    this._F_CHQ_ACCT_SERIAL_NO_BBG_SN.release();
    this._R_COM.release();
    this._sPos.release();
    this._FN_CHQ_ISSUE_HIS_BBG_SN.release();
    this._N_R_LINE_DETAIL.release();
    this._CI_PRINT_POS.release();
    this._Y_FILE_UPDATED.release();
    this._POS.release();
    this._Y_CHEQUE_DESCRIPTION.release();
    this._Y_EXT_ID_LIST.release();
    this._Y_PARAM_VALUES.release();
    this._REC_LOCK_ERR.release();
    this._AC_ID.release();
    this._N_VAL_DEF.release();
    this._N_R_FINAL_ARRAY.release();
    this._R_INT_EXT.release();
    this._COMP_ADDR4.release();
    this._COMP_ADDR3.release();
    this._COMP_ADDR2.release();
    this._COMP_ADDR1.release();
    this._N_ORDER_SEQ_NO.release();
    this._F_ACCOUNT.release();
    this._F_CHQ_INTERFACE_EXTRACT_BBG_SN.release();
    this._FN_CHQ_INTERFACE_EXTRACT_BBG_SN.release();
    this._CODE_GUICHET.release();
    this._CI_CROSSING_POS.release();
    this._FN_CHEQUE_ISSUE.release();
    this._FN_LOCKING.release();
    this._CI_CNT.release();
    this._PASSED_NO.release();
    this._FN_CHQ_ISSUE_FAILED_BBG_SN.release();
    this._SELECT_LIST.release();
    this._N_BBG_GROUP.release();
    this._FN_CHQ_ACCT_SERIAL_NO_BBG_SN.release();
    this._CIT_ERR.release();
    this._F_CHQ_DATA_FILE_BBG_SN.release();
    this._AC_ERR.release();
    this._N_CROSS_CHQ.release();
    this._N_MODEL_CODE.release();
    this._Y_FILE_NAME_PREFIX.release();
    this._R_DATA_FILE.release();
    this._R_CI.release();
    this._FLD_LIST.release();
    this._CIS_ERR.release();
    this._Y_CHQ_NO_LEAVES.release();
    this._N_BBG_GROUP_CI.release();
    this._R_CIT.release();
    this._FN_CHQ_ISSUE_SEQ_BBG_SN.release();
    this._R_CIH.release();
    this._FLD_POS.release();
    this._N_BARRE_NO_EXT.release();
    this._RET_CODE.release();
    this._FN_CHQ_ISSUE_TODAY_BBG_SN.release();
    this._F_LOCKING_CHQ.release();
    this._N_branch_Code.release();
    this._R_LOCK_T.release();
    this._REQ_COMMITTED.release();
    this._OFS_RESPONSE.release();
    this._N_FILE_H_BEGIN_ID.release();
    this._CI_PID_POS.release();
    this._Y_CI_INDEX_SEQ_NO.release();
    this._R_LOCK.release();
    this._COMP_ID.release();
    this._S_PARAM_VALUES.release();
    this._N_Endossable.release();
    this._NAME_ON_CHQ.release();
    this._F_COMPANY.release();
    this._N_CHQ_IN.release();
    this._COMP_ADDR.release();
    this._Y_CI_INDEX_LOCK_ID.release();
    this._ERR_LOCKING.release();
    this._Y_CHEQUE_TYPE.release();
    this._NO_OF_RECS.release();
    this._CI_ERR.release();
    this._Y_LOCK_ID.release();
    this._N_OLD_CHQ_TYPE.release();
    this._Y_SEQ_NO.release();
    this._CI_ACCT_POS.release();
    this._PRINTER_ID.release();
    this._Y_OFS_SOURCE_ID.release();
    this._OFS_REC.release();
    this._Y_CQ_PROCESS_STATUS.release();
    this._S_RETURN_CODE.release();
    this._CI_CQ_START_POS.release();
    this._CI_ADD3_POS.release();
    this._Y_SR_NO.release();
    this._SELECT_CMD.release();
    this._F_FILE_OUT_DIR.release();
    this._Y_CHQ_TYPE_LIST.release();
    this._FN_CHQ_DATA_FILE_BBG_SN.release();
    this._CHQ_TELNO.release();
  }
  
  protected void GOSUB(int paramInt) {
    GOSUB(paramInt, true);
  }
  
  protected void GOSUB(int paramInt, boolean paramBoolean) {
    int i = -1;
    if (paramBoolean)
      try {
        checkCallStack("-gs:" + getLabelName(paramInt));
      } catch (Exception exception) {
        this.session.setStateSubroutine(-3);
        paramInt = -3;
        i = -3;
      }  
    switch (paramInt) {
      case 0:
        i = main();
        break;
    } 
    check(i);
  }
  
  protected void CB(int paramInt) {
    GOSUB(paramInt, false);
  }
  
  private String getLabelName(int paramInt) {
    return (paramInt == 1) ? "GET_PARAM_VALUES" : ((paramInt == 2) ? "POST_OFS_BULK" : ((paramInt == 3) ? "BOOK_OUT_PATH" : ((paramInt == 4) ? "UPDATE_CONCAT_HISTORY" : ((paramInt == 7) ? "GET_CHEQUE_DESCRIPTION" : ((paramInt == 9) ? "GET_ACCOUNT_RIB" : ((paramInt == 10) ? "FIND_CROSS_CHECK_ENDO" : ((paramInt == 11) ? "UPDATE_DATA_FILE" : ((paramInt == 12) ? "GET_CHQ_SEQUENCE_NUMBER" : ((paramInt == 13) ? "GET_COMPANY_NAME" : ((paramInt == 14) ? "UPDATE_CHEQUE_ISSUE" : ((paramInt == 15) ? "OPENFILES" : ((paramInt == 16) ? "UPDATE_HEADER_DET" : ((paramInt == 17) ? "MAIN_PROCESS" : ((paramInt == 18) ? "GET_CHEQUE_ISSUE_DETAIL" : ((paramInt == 19) ? "INITIALISE" : "")))))))))))))));
  }
  
  public String[] getParamList() {
    if (_paramList_ == null)
      _paramList_ = new String[0]; 
    return _paramList_;
  }
  
  public jVar[] getParams() {
    return new jVar[0];
  }
  
  public static String[] getParamListStatic() {
    if (_paramList_ == null)
      _paramList_ = new String[0]; 
    return _paramList_;
  }
  
  public jVar getDataStructureNames() {
    StringBuilder stringBuilder = new StringBuilder();
    boolean bool = true;
    for (Field field : getClass().getFields()) {
      if (field.getName().startsWith("_h__i__d__d__e__n__fields_")) {
        if (!bool)
          stringBuilder.append(sFM); 
        bool = false;
        stringBuilder.append(field.getName().substring("_h__i__d__d__e__n__fields_".length()).replace('_', '.'));
      } 
    } 
    return jVarFactory.get(stringBuilder.toString());
  }
  
  public jVar getDataStructureFields(Object paramObject) {
    String str = paramObject.toString();
    StringBuilder stringBuilder = new StringBuilder();
    try {
      Field field = getClass().getField("_h__i__d__d__e__n__fields_" + str.replace('.', '_'));
      if (field != null) {
        String[] arrayOfString = (String[])field.get(null);
        boolean bool = true;
        for (String str1 : arrayOfString) {
          if (!bool)
            stringBuilder.append(sFM); 
          bool = false;
          stringBuilder.append(str1);
        } 
      } 
    } catch (Exception exception) {}
    return jVarFactory.get(stringBuilder.toString());
  }
  
  public static String[] getDependenciesStatic() {
    String[] arrayOfString = new String[9];
    arrayOfString[0] = "F.DELETE";
    arrayOfString[1] = "EB.READLIST";
    arrayOfString[2] = "U.GET.PARAM.VALUE.BBG";
    arrayOfString[3] = "MULTI.GET.LOC.REF";
    arrayOfString[4] = "OPF";
    arrayOfString[5] = "F.READ";
    arrayOfString[6] = "OFS.POST.MESSAGE";
    arrayOfString[7] = "OCOMO";
    arrayOfString[8] = "F.WRITE";
    return arrayOfString;
  }
  
  public String getPathFileNameBasic() {
    return "/t24appl/t24migdev/t24/bnk/UD/BBG.BP/B.CEMPA.CQ.ORDER.FILE.BBG.CIV.b";
  }
  
  public static String getPathFileNameBasicStatic() {
    return "/t24appl/t24migdev/t24/bnk/UD/BBG.BP/B.CEMPA.CQ.ORDER.FILE.BBG.CIV.b";
  }
  
  public String getCompileInfo() {
    return "1721907123752\t25 Jul 2024 11:32:03\tBBGMIGDEVAPP01\t3";
  }
  
  public static String getCompileInfoStatic() {
    return "1721907123752\t25 Jul 2024 11:32:03\tBBGMIGDEVAPP01\t3";
  }
  
  public String getPackageBasic() {
    return "";
  }
  
  public static String getPackageBasicStatic() {
    return "";
  }
  
  public String getImportBasic() {
    return "com.temenos.t24";
  }
  
  public static String getImportBasicStatic() {
    return "com.temenos.t24";
  }
  
  public String getVersion() {
    return "R21_SP1.0";
  }
  
  public static String getVersionStatic() {
    return "R21_SP1.0";
  }
  
  public String getReplacementInfo() {
    return "false";
  }
}
