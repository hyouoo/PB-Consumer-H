package com.example.purebasketconsumer.consumer;


import com.example.purebasketconsumer.consumer.dto.KafkaEventDto;
import com.example.purebasketconsumer.domain.member.MemberRepository;
import com.example.purebasketconsumer.global.sse.EmailContents;
import com.example.purebasketconsumer.global.sse.SseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumer {

    private final MemberRepository memberRepository;
    private final JavaMailSender javaMailSender;
    private final SseRepository sseRepository;

    @KafkaListener(topics = "${spring.kafka.consumer.topics.event}", groupId = "${spring.kafka.consumer.group-id.event}",
            containerFactory = "kafkaEventListenerContainerFactory", concurrency = "1")
    public void sendEmailToMembers(KafkaEventDto responseDto) {
        log.info("Method called : sendEmailToMembers");
        List<String> emailList = memberRepository.findAllEmails();
        EmailContents contents = EmailContents.from(responseDto);
        String subject = contents.subject();
        String text = contents.text();

        for (String email : emailList) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(text);
            javaMailSender.send(message);
        }
        log.info("메일 전송 완료");

    }

    @KafkaListener(topics = "${spring.kafka.consumer.topics.event}", groupId = "${spring.kafka.consumer.group-id.event}",
            containerFactory = "kafkaEventListenerContainerFactory" ,concurrency = "3")
    private void alarmNewEvent(KafkaEventDto responseDto) {
        Map<String, SseEmitter> connectionMap = sseRepository.findAllEmitters();
//                int salePrice = responseDto.price() * responseDto.discountRate() / 100;
        String message = String.format("""
                        새로운 할인 이벤트!
                        %s %d%% 할인!! 한정수량 단 %d개!!!""",
                responseDto.name(), responseDto.discountRate(), responseDto.stock());

        if (!connectionMap.isEmpty()) {
            connectionMap.forEach((id, emitter) -> notify(emitter, id, message));
        }
    }

    private void notify(SseEmitter emitter, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .data(data)); // String Type만 가능함
        } catch (IOException e) {
            log.error("notify 실패 : {}", e.getMessage());
            sseRepository.delete(emitterId);
        }
    }

}
