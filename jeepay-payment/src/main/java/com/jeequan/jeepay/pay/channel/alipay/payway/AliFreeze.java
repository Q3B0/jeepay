package com.jeequan.jeepay.pay.channel.alipay.payway;

import cn.hutool.json.JSONUtil;
import com.alipay.api.domain.AlipayFundAuthOrderFreezeModel;
import com.alipay.api.request.AlipayFundAuthOrderFreezeRequest;
import com.alipay.api.response.AlipayFundAuthOrderFreezeResponse;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.utils.AmountUtil;
import com.jeequan.jeepay.pay.channel.alipay.AlipayKit;
import com.jeequan.jeepay.pay.channel.alipay.AlipayPaymentService;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.AbstractRS;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRQ;
import com.jeequan.jeepay.pay.rqrs.payorder.payway.AliBarOrderRQ;
import com.jeequan.jeepay.pay.rqrs.payorder.payway.AliFreezeOrderRQ;
import com.jeequan.jeepay.pay.rqrs.payorder.payway.AliOcOrderRS;
import com.jeequan.jeepay.pay.util.ApiResBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
@Slf4j
@Service("alipayPaymentByAliFreezeService")
public class AliFreeze extends AlipayPaymentService {
    @Override
    public String preCheck(UnifiedOrderRQ rq, PayOrder payOrder) {
        AliFreezeOrderRQ bizRQ = (AliFreezeOrderRQ) rq;
        if(StringUtils.isEmpty(bizRQ.getAuthCode())){
            throw new BizException("用户支付条码[authCode]不可为空");
        }

        return null;
    }

    @Override
    public AbstractRS pay(UnifiedOrderRQ rq, PayOrder payOrder, MchAppConfigContext mchAppConfigContext) {

        AliFreezeOrderRQ bizRQ = (AliFreezeOrderRQ) rq;

        AlipayFundAuthOrderFreezeRequest req = new AlipayFundAuthOrderFreezeRequest();
        AlipayFundAuthOrderFreezeModel model = new AlipayFundAuthOrderFreezeModel ();
        // 构造请求参数以调用接口
        //设置商户订单号
        model.setOutOrderNo(payOrder.getPayOrderId());
        model.setOutRequestNo(payOrder.getPayOrderId());
        //设置需要冻结的金额
        model.setAmount(AmountUtil.convertCent2Dollar(payOrder.getAmount().toString()));
        // 设置订单标题
        model.setOrderTitle(payOrder.getSubject());
        // 设置产品码
        model.setProductCode("PREAUTH_PAY");
        // 设置免押受理台模式 POSTPAY 表示后付金额已知，POSTPAY_UNCERTAIN 表示后付金额未知，DEPOSIT_ONLY 表示纯免押
        model.setDepositProductMode("DEPOSIT_ONLY");
        //固定值传入 bar_code
        model.setAuthCodeType("bar_code");
        //用户付付款码
        model.setAuthCode(bizRQ.getAuthCode().trim());

        log.warn(JSONUtil.toJsonStr(model));
        //设置异步通知地址
        req.setNotifyUrl(getNotifyUrl());
        req.setBizModel(model);
        //统一放置 isv接口必传信息
        AlipayKit.putApiIsvInfo(mchAppConfigContext, req, model);
        //调起支付宝 （如果异常， 将直接跑出   ChannelException ）
        AlipayFundAuthOrderFreezeResponse alipayResp = configContextQueryService.
                getAlipayClientWrapper(mchAppConfigContext).execute(req);
        AliOcOrderRS res = ApiResBuilder.buildSuccess(AliOcOrderRS.class);
        ChannelRetMsg channelRetMsg = new ChannelRetMsg();
        res.setChannelRetMsg(channelRetMsg);

        //放置 响应数据
        channelRetMsg.setChannelAttach(alipayResp.getBody());
        channelRetMsg.setChannelOrderId(alipayResp.getAuthNo());
        channelRetMsg.setChannelUserId(alipayResp.getPayerLogonId()); //渠道用户标识

        // ↓↓↓↓↓↓ 调起接口成功后业务判断务必谨慎！！ 避免因代码编写bug，导致不能正确返回订单状态信息  ↓↓↓↓↓↓

        //当条码重复发起时，支付宝返回的code = 10003, subCode = null [等待用户支付], 此时需要特殊判断 = = 。
        if("10000".equals(alipayResp.getCode()) && alipayResp.isSuccess()){ //支付成功, 更新订单成功 || 等待支付宝的异步回调接口

            channelRetMsg.setChannelState(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS);


        }else if("10003".equals(alipayResp.getCode())){ //10003 表示为 处理中, 例如等待用户输入密码

            channelRetMsg.setChannelState(ChannelRetMsg.ChannelState.WAITING);

        }else{  //其他状态, 表示下单失败
            channelRetMsg.setChannelState(ChannelRetMsg.ChannelState.CONFIRM_FAIL);
            channelRetMsg.setChannelErrCode(AlipayKit.appendErrCode(alipayResp.getCode(), alipayResp.getSubCode()));
            channelRetMsg.setChannelErrMsg(AlipayKit.appendErrMsg(alipayResp.getMsg(), alipayResp.getSubMsg()));
        }

        return res;

    }
}
