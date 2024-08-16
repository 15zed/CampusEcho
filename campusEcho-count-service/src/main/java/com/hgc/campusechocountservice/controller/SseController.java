package com.hgc.campusechocountservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/count")
public class SseController {

    private List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/sse/connect")
    public SseEmitter connect() {
        SseEmitter emitter = new SseEmitter();
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    private void sendEvent(String eventName, String message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(message));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    public void sendLikesUpdate(Integer infoId, Long likesCount) {
        String message = infoId + ":" + likesCount;
        sendEvent("likesUpdate", message);
    }

    public void sendCommentsUpdate(Integer infoId, Long commentsCount) {
        String message = infoId + ":" + commentsCount;
        sendEvent("commentsUpdate", message);
    }

    public void sendFollowersUpdate(Integer userId, Long followersCount) {
        String message = userId + ":" + followersCount;
        sendEvent("followersUpdate", message);
    }

    public void sendFollowingUpdate(Integer userId, Long followingCount) {
        String message = userId + ":" + followingCount;
        sendEvent("followingUpdate", message);
    }
}
