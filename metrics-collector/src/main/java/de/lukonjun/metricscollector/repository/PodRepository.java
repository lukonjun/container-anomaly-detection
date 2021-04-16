package de.lukonjun.metricscollector.repository;

import de.lukonjun.metricscollector.model.ModelPodMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PodRepository extends JpaRepository<ModelPodMetrics,Long> {

}