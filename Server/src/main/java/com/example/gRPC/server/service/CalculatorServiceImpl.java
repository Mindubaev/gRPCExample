package com.example.gRPC.server.service;

import com.example.gRPC.api.CalculatorServiceGrpc;
import com.example.gRPC.api.FindRootRequest;
import com.example.gRPC.api.FindRootResponse;
import com.example.gRPC.api.Number;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class CalculatorServiceImpl extends CalculatorServiceGrpc.CalculatorServiceImplBase {

    @Override
    public void pow(FindRootRequest request, StreamObserver<FindRootResponse> responseObserver) {
        double result=Math.pow(request.getN(),request.getExp());
        FindRootResponse response=FindRootResponse.newBuilder()
                .setResult(result)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Number> findDeviation(StreamObserver<Number> responseObserver) {
        return new StreamObserver<Number>() {

            List<Double> nums=new ArrayList<>();
            double aver=0;

            @Override
            public void onNext(Number number) {
                double num=number.getNum();
                aver+=num;
                nums.add(num);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                try {
                    double d=0;
                    aver/=nums.size();
                    for (Double num:nums)
                        d+=Math.pow(num-aver,2);
                    d=Math.pow(d/nums.size(),1d/2);
                    Number result=Number.newBuilder().setNum(d).build();
                    Thread.sleep(1000);
                    responseObserver.onNext(result);
                    responseObserver.onCompleted();
                }catch(InterruptedException e) {
                    e.printStackTrace();
                    responseObserver.onError(e);
                }
            }
        };
    }

    @Override
    public void findMultipliers(Number request, StreamObserver<Number> responseObserver) {
        double result=request.getNum();
        double rb=Math.round(Math.pow(result,1d/2));
        for (double i=2;i<=rb;i++){
            while (result%i==0){
                responseObserver.onNext(Number.newBuilder().setNum(i).build());
                result/=i;
            }
            if (result==1){
                responseObserver.onNext(Number.newBuilder().setNum(1).build());
                break;
            }
        }
        if (result!=1) {
            responseObserver.onNext(Number.newBuilder().setNum(1).build());
            responseObserver.onNext(Number.newBuilder().setNum(result).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Number> findMax(StreamObserver<Number> responseObserver) {
        return new StreamObserver<Number>() {
            Double max=null;

            @Override
            public void onNext(Number number) {
                double req=number.getNum();
                if (max==null || req>max)
                    max=req;
                responseObserver.onNext(Number.newBuilder().setNum(max).build());
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
