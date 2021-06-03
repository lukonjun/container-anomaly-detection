package de.lukonjun.metricscollector.controller;

import de.lukonjun.metricscollector.kubernetes.ApiConnection;
import io.kubernetes.client.ProtoClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.proto.V1;

import java.io.IOException;

public class TimeController {

    public static void main(String[] args) throws IOException, ApiException {
        ProtoClient pc = new ProtoClient(new ApiConnection().createConnection().getClient());
        final ProtoClient.ObjectOrStatus<V1.PodList> podList = pc.list(V1.PodList.newBuilder(), "/api/v1/pods");
        for (V1.Pod pod : podList.object.getItemsList()) {
            System.out.println("StartTime" + pod.getStatus().getStartTime());
        }
    }

}
