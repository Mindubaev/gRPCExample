package com.example.gRPC.client;

import com.example.gRPC.api.CalculatorServiceGrpc;
import com.example.gRPC.api.FindRootRequest;
import com.example.gRPC.api.FindRootResponse;
import com.example.gRPC.api.Number;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Application {

    public static void main(String[] args){
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9000)
                .usePlaintext()
                .build();

        CalculatorServiceGrpc.CalculatorServiceBlockingStub calculatorServiceBlocking=CalculatorServiceGrpc
                .newBlockingStub(channel);

        testSimpleRPC(calculatorServiceBlocking,81,1d/2);

        CalculatorServiceGrpc.CalculatorServiceStub calculatorServiceAsync=CalculatorServiceGrpc.newStub(channel);

        testClientStreaming(calculatorServiceAsync,5);

        testServerStreaming(calculatorServiceAsync,11);

        testBidirectionalStreaming(calculatorServiceAsync,10,1000);

        channel.shutdown();
    }

    private static void testSimpleRPC(CalculatorServiceGrpc.CalculatorServiceBlockingStub calculatorService,double num,double exp){
        FindRootRequest request=FindRootRequest
                .newBuilder()
                .setN(num)
                .setExp(exp)
                .build();

        FindRootResponse response=calculatorService.pow(request);
        System.out.println(num+"^"+exp+"="+response.getResult());
    }

    private static void testClientStreaming(CalculatorServiceGrpc.CalculatorServiceStub calculatorService,int n){
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<Number> responseObserver=new StreamObserver<Number>() {
            @Override
            public void onNext(Number number) {
                System.out.println("Отклонение ="+number.getNum());
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Процедура подсчёта завершена");
                finishLatch.countDown();
            }
        };
        StreamObserver<Number> requestObserver=calculatorService.findDeviation(responseObserver);
        try {
            for (int i = 0; i < n; i++) {
                double rand = Math.random() * 10;
                System.out.println("отправка " + rand);
                requestObserver.onNext(Number.newBuilder().setNum(rand).build());
                if (finishLatch.getCount() == 0) {
                    return;
                }
            }
            requestObserver.onCompleted();
            finishLatch.await(1, TimeUnit.MINUTES);
        }catch (RuntimeException | InterruptedException e){
            e.printStackTrace();
            requestObserver.onError(e);
        }
    }

    private static void testServerStreaming(CalculatorServiceGrpc.CalculatorServiceStub calculatorService,double num){
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<Number> responseObserver=new StreamObserver<Number>() {
            List<Double> mults=new ArrayList<>();
            @Override
            public void onNext(Number number) {
                mults.add(number.getNum());
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("множетели"+mults.toString());
                finishLatch.countDown();
            }
        };
        try {
            calculatorService.findMultipliers(Number.newBuilder().setNum(num).build(), responseObserver);
            finishLatch.await(1, TimeUnit.MINUTES);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private static void testBidirectionalStreaming(CalculatorServiceGrpc.CalculatorServiceStub calculatorService,int numOfPackets,double range){
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<Number> responseObserver=new StreamObserver<Number>() {
            int count=0;
            @Override
            public void onNext(Number number) {
                System.out.println("Текущий минимум="+number.getNum());
                count++;
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                System.out.println("Подсчёт максимума закочен с ошибкой. Полученно пакетов:"+count);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Подсчёт максимума закочен успешно. Полученно пакетов:"+count);
                finishLatch.countDown();
            }
        };
        StreamObserver<Number> requestObserver = calculatorService.findMax(responseObserver);
        try {
            for (int i = 0; i < numOfPackets; i++) {
                double num = Math.random() * range;
                requestObserver.onNext(Number.newBuilder().setNum(num).build());
                System.out.println("Отправленно: " + num);
            }
            requestObserver.onCompleted();
            if (finishLatch.getCount() != 0)
                finishLatch.await(1, TimeUnit.MINUTES);
        }catch (InterruptedException e){
            e.printStackTrace();
            requestObserver.onError(e);
        }
    }

}
