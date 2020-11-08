package com.example.gRPC.server;

import com.example.gRPC.server.service.CalculatorServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class Application {

    public static void main(String[] args){
        try {
            Server server = ServerBuilder.forPort(9000)
                    .addService(new CalculatorServiceImpl())
                    .build();
            server.start();
            server.awaitTermination();
        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

}
