package com.example.scrimish;

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

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RoomManager implements RoomListener, ObservableRoomListener {

    private static RoomManager instance;
    private Room room;
    private Callback mCallback;
    private Scaledrone channel;
    private MembershipListener mMembershipListener;
    public long mLastSentTimestamp;

    private static final String BASE_URL = "https://api2.scaledrone.com/";

    public static RoomManager getInstance() {
        if (instance == null) instance = new RoomManager();
        return instance;
    }

    public static void getAllRooms(String channelId, retrofit2.Callback<Map<String, List<String>>> callback) {
        retrofit(channelId).create(Service.class).groupList().enqueue(callback);
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

    public void connectToRoom(final String channnelId, final String room) {
        channel = new Scaledrone(channnelId);
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

    public void clearCallback() {
        mCallback = null;
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

    public static Retrofit retrofit(String channelId) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL + channelId + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public interface Callback {
        void onMessageReceived(String s, long timestamp);
    }

    public interface MembershipListener {
        void onMembers(int size);

        void onMemberLeave();
    }

}
