package com.tvd12.ezyfoxserver.client.constant;

import java.util.Map;

import com.tvd12.ezyfox.constant.EzyConstant;
import com.tvd12.ezyfox.util.EzyEnums;

public enum EzyDisconnectReason implements EzyConstant {

	CLOSE(-1),
	UNKNOWN(0),
	IDLE(1),
	NOT_LOGGED_IN(2),
	ANOTHER_SESSION_LOGIN(3),
	ADMIN_BAN(4),
    ADMIN_KICK(5),
    MAX_REQUEST_PER_SECOND(6),
    MAX_REQUEST_SIZE(7),
    SERVER_ERROR(8),
	SERVER_NOT_RESPONDING(400),
	UNAUTHORIZED(401);

	private final int id;
	
	private final static Map<Integer, EzyDisconnectReason> MAP = 
			EzyEnums.enumMapInt(EzyDisconnectReason.class);
	
	private EzyDisconnectReason(int id) {
		this.id = id;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getName() {
		return toString();
	}
	
	public static EzyDisconnectReason valueOf(int id) {
		return MAP.get(Integer.valueOf(id));
	}
	
}
