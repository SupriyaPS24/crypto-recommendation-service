package com.xm.cryptoservice.repository;

import com.xm.cryptoservice.model.LoadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadedFileRepository extends JpaRepository<LoadedFile, String> {

}
