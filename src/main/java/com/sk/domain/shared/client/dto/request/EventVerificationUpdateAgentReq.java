package com.sk.domain.shared.client.dto.request;

import java.util.UUID;

public record EventVerificationUpdateAgentReq(UUID verificationId, String actorUsrLognId, String expectedUpdatedAt) {}
