package cn.kaoqin.vo;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 考勤对象VO类
 * @author ZhaoZhigang
 *
 */
@Data
@SuppressWarnings("unused")
public class KaoQinRecordVo {
    /** 支出大类 */
    private String expenditureCategories;
    /** 支出小类 */
    private String expenditureCategory;
    /** 账户 */
    private String record;
    /** 币种 */
    private String currency;
    /** 项目 */
    private String project;
    /** 商家 */
    private String business;
    /** 报销 */
    private String reimbursement;
    /** 消费日期 */
    private Date consumptionDate;
    /** 消费金额 */
    private BigDecimal consumptionAmount;
    /** 成员金额 */
    private String memberAmount;
    /** 备注 */
    private String remarks;
    /** 考勤 */
    private String recordBook;
    
    /** 收/支 */
    private String collectionOrSupport;
    /** 交易对方 */
    private String tradingParty;
    /** 商品 */
    private String commodity;
}
