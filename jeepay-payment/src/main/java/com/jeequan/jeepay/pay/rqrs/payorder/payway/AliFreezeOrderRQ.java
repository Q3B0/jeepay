package com.jeequan.jeepay.pay.rqrs.payorder.payway;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRQ;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AliFreezeOrderRQ extends UnifiedOrderRQ {
    /** 用户 支付条码 **/
    @NotBlank(message = "支付条码不能为空")
    private String authCode;

    /** 构造函数 **/
    public AliFreezeOrderRQ(){
        this.setWayCode(CS.PAY_WAY_CODE.ALI_FREEZE); //默认 ALI_FREEZE, 避免validate出现问题
    }
}
