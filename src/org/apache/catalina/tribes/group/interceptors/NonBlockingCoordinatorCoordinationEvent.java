package org.apache.catalina.tribes.group.interceptors;

import org.apache.catalina.tribes.ChannelInterceptor;
import org.apache.catalina.tribes.ChannelInterfaceptorInterceptorEvent;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.membership.Membership;
import org.apache.catalina.tribes.util.Arrays;

public class NonBlockingCoordinatorCoordinationEvent implements
		ChannelInterfaceptorInterceptorEvent {
	private static final int EVT_START = 1;
	private static final int EVT_MBR_ADD = 2;
	private static final int EVT_MBR_DEL = 3;
	private static final int EVT_START_ELECT = 4;
	private static final int EVT_PROCESS_ELECT = 5;
	private static final int EVT_MSG_ARRIVE = 6;
	private static final int EVT_PRE_MERGE = 7;
	private static final int EVT_POST_MERGE = 8;
	private static final int EVT_WAIT_FOR_MSG = 9;
	private static final int EVT_SEND_MSG = 10;
	private static final int EVT_STOP = 11;
	private static final int EVT_CONF_RX = 12;
	private static final int EVT_ELECT_ABANDONED = 13;

	private int type;
	private ChannelInterceptor interceptor;
	private Member coord;
	private Member[] mbrs;
	private String info;
	private Membership view;
	private Membership suggestedView;

	public NonBlockingCoordinatorCoordinationEvent(int type,
			ChannelInterceptor interceptor, String info) {
		this.type = type;
		this.interceptor = interceptor;
		this.coord = ((NonBlockingCoordinator) interceptor).getCoordinator();
		this.mbrs = ((NonBlockingCoordinator) interceptor).getMembership()
				.getMembers();
		this.info = info;
		this.view = ((NonBlockingCoordinator) interceptor).getViewVariable();
		this.suggestedView = ((NonBlockingCoordinator) interceptor).getSuggestedView();
	}

	@Override
	public int getEventType() {
		return type;
	}

	@Override
	public String getEventTypeDesc() {
		switch (type) {
		case EVT_START:
			return "EVT_START:" + info;
		case EVT_MBR_ADD:
			return "EVT_MBR_ADD:" + info;
		case EVT_MBR_DEL:
			return "EVT_MBR_DEL:" + info;
		case EVT_START_ELECT:
			return "EVT_START_ELECT:" + info;
		case EVT_PROCESS_ELECT:
			return "EVT_PROCESS_ELECT:" + info;
		case EVT_MSG_ARRIVE:
			return "EVT_MSG_ARRIVE:" + info;
		case EVT_PRE_MERGE:
			return "EVT_PRE_MERGE:" + info;
		case EVT_POST_MERGE:
			return "EVT_POST_MERGE:" + info;
		case EVT_WAIT_FOR_MSG:
			return "EVT_WAIT_FOR_MSG:" + info;
		case EVT_SEND_MSG:
			return "EVT_SEND_MSG:" + info;
		case EVT_STOP:
			return "EVT_STOP:" + info;
		case EVT_CONF_RX:
			return "EVT_CONF_RX:" + info;
		case EVT_ELECT_ABANDONED:
			return "EVT_ELECT_ABANDONED:" + info;
		default:
			return "Unknown";
		}
	}

	@Override
	public ChannelInterceptor getInterceptor() {
		return interceptor;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(
				"NonBlockingCoordinatorCoordinationEvent[type=");
		buf.append(type).append("\n\tLocal:");
		Member local = interceptor.getLocalMember(false);
		buf.append(local != null ? local.getName() : "").append("\n\tCoord:");
		buf.append(coord != null ? coord.getName() : "").append("\n\tView:");
		buf.append(Arrays.toNameString(view != null ? view.getMembers() : null))
				.append("\n\tSuggested View:");
		buf.append(
				Arrays.toNameString(suggestedView != null ? suggestedView
						.getMembers() : null)).append("\n\tMembers:");
		buf.append(Arrays.toNameString(mbrs)).append("\n\tInfo:");
		buf.append(info).append("]");
		return buf.toString();
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Member getCoord() {
		return coord;
	}

	public void setCoord(Member coord) {
		this.coord = coord;
	}

	public Member[] getMbrs() {
		return mbrs;
	}

	public void setMbrs(Member[] mbrs) {
		this.mbrs = mbrs;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public Membership getView() {
		return view;
	}

	public void setView(Membership view) {
		this.view = view;
	}

	public Membership getSuggestedView() {
		return suggestedView;
	}

	public void setSuggestedView(Membership suggestedView) {
		this.suggestedView = suggestedView;
	}

	public static int getEvtStart() {
		return EVT_START;
	}

	public static int getEvtMbrAdd() {
		return EVT_MBR_ADD;
	}

	public static int getEvtMbrDel() {
		return EVT_MBR_DEL;
	}

	public static int getEvtStartElect() {
		return EVT_START_ELECT;
	}

	public static int getEvtProcessElect() {
		return EVT_PROCESS_ELECT;
	}

	public static int getEvtMsgArrive() {
		return EVT_MSG_ARRIVE;
	}

	public static int getEvtPreMerge() {
		return EVT_PRE_MERGE;
	}

	public static int getEvtPostMerge() {
		return EVT_POST_MERGE;
	}

	public static int getEvtWaitForMsg() {
		return EVT_WAIT_FOR_MSG;
	}

	public static int getEvtSendMsg() {
		return EVT_SEND_MSG;
	}

	public static int getEvtStop() {
		return EVT_STOP;
	}

	public static int getEvtConfRx() {
		return EVT_CONF_RX;
	}

	public static int getEvtElectAbandoned() {
		return EVT_ELECT_ABANDONED;
	}

	public void setInterceptor(ChannelInterceptor interceptor) {
		this.interceptor = interceptor;
	}

}