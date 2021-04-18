package com.example.roommanager;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Network {

    private static final String BASE_URL = "https://api2.scaledrone.com/" + RoomManager.CHANNEL_ID + "/";

    public static Retrofit builder() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

}
