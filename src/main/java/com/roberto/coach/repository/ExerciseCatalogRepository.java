package com.roberto.coach.repository;

import com.roberto.coach.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

// 2. Exercise Catalog
public interface ExerciseCatalogRepository extends JpaRepository<ExerciseCatalog, String> {
    @Query("SELECT e FROM ExerciseCatalog e WHERE e.equipmentNeeded IN :equipmentList")
    List<ExerciseCatalog> findByEquipmentAllowed(@Param("equipmentList") List<String> equipmentList);
}