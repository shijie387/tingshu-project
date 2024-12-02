package com.atguigu.tingshu.account;


import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Slf4j
@Component
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_USER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_USER_REGISTER, durable = "true"),
            key = MqConst.ROUTING_USER_REGISTER
    ))

    public void initUserAccount(Map<String,Object> mapData, Message message, Channel channel) {
        if (CollectionUtil.isNotEmpty(mapData)) {
            //1. 处理业务
            log.info("[账户服务]监听到用户首次注册初始化账户消息：{}", mapData);
            userAccountService.saveUserAccount(mapData);
        }
        //2.手动确认消息 参数1：为投递消息分配唯一标识 参数2：batch process?
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
