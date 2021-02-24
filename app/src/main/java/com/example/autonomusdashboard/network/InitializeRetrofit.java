package com.example.autonomusdashboard.network;

import com.example.autonomusdashboard.utils.Constants;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class InitializeRetrofit {

    private static Retrofit retrofitInstance;

    public static Retrofit getInstance(){
        if(retrofitInstance == null){
            retrofitInstance = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        }

        return retrofitInstance;

    }

}
