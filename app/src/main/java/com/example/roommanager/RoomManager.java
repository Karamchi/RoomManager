package com.example.roommanager;

import android.util.Log;

import com.scaledrone.lib.Listener;
import com.scaledrone.lib.Member;
import com.scaledrone.lib.Message;
import com.scaledrone.lib.ObservableRoomListener;
import com.scaledrone.lib.Room;
import com.scaledrone.lib.RoomListener;
import com.scaledrone.lib.Scaledrone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoomManager implements RoomListener, ObservableRoomListener {

    /** casos:
     * 2 conectados, los dos se van sin notificar: queda una room con 2 jugadores que no se va a volver a usar
     * Con la solucion actual: eventualmente pasan 60 segundos, alguein hace una request y listo
     * 2 conectados, uno se va sin notificar: queda una room con 2 jugadores que no se va a volver a usar. El que quedó no va a poder matchear con nadie
     * Con la solución actual: el que quedó updatea que la room todavía tiene 1
     * 1 conectado, se va sin notificar: queda una room con 1 jugador. El próximo que entre va a ir a esa room y no va a poder matchear
     * Con la solución actual: el próximo que entre va a updatear que la room todavía tiene 1. Si nadie entra la room desaparece
     */

    private static RoomManager instance;
    private Room room;
    private Callback mCallback;
    private Scaledrone channel;
    private MembershipListener mMembershipListener;
    public long mLastSentTimestamp;

    public final static String CHANNEL_ID = "";

    public static RoomManager getInstance() {
        if (instance == null) instance = new RoomManager();
        return instance;
    }

    public static void getAllRooms(retrofit2.Callback<Map<String, List<String>>> callback) {
        Network.builder().create(Service.class).groupList().enqueue(callback);
    }

    public void sendMessage(String message) {
        if (message == null) {
            Log.e("null", "msg");
            return;
        }
        if (room != null)
            room.publish(message);
    }

    public void registerCallback(Callback callback) {
        mCallback = callback;
    }

    public void connectToRoom(final String room) {
        channel = new Scaledrone(CHANNEL_ID);
        Log.e("room", room);
        channel.connect(new Listener() {
            @Override
            public void onOpen() {
                Log.e("room", room + "$");
                channel.subscribe(room, RoomManager.this);
            }

            @Override
            public void onOpenFailure(Exception ex) {
                ex.printStackTrace();
            }

            @Override
            public void onFailure(Exception ex) {
            }

            @Override
            public void onClosed(String reason) {
            }
        });
    }

    public void disconnect() {
        if (room == null) return;
        channel.unsubscribe(room);
        Log.e("Disconnecting from", room.getName());
    }

    @Override
    public void onOpen(Room room) {
        this.room = room;
        room.listenToObservableEvents(this);
        if (mMembershipListener != null)
            mMembershipListener.onMembers(room.getMembers().size());
    }

    @Override
    public void onOpenFailure(Room room, Exception ex) {}

    @Override
    public void onMessage(Room room, Message message) {
        Log.e("msg", message.getData().asText());
        if (message.getClientID().equals(channel.getClientID())) {
            mLastSentTimestamp = message.getTimestamp();
        } else if (mCallback != null) {
            Log.e("msg", "handled");
            mCallback.onMessageReceived(message.getData().asText(), message.getTimestamp());
        }
    }

    public void setMembershipListener(MembershipListener listener) {
        mMembershipListener = listener;
    }

    @Override
    public void onMembers(Room room, ArrayList<Member> members) {
    }

    @Override
    public void onMemberJoin(Room room, Member member) {
        if (mMembershipListener != null)
            mMembershipListener.onMembers(room.getMembers().size());
    }

    @Override
    public void onMemberLeave(Room room, Member member) {
        if (mMembershipListener != null)
            mMembershipListener.onMemberLeave();
    }

    public interface Callback {
        void onMessageReceived(String s, long timestamp);
    }

    public interface MembershipListener {
        void onMembers(int size);

        void onMemberLeave();
    }

}