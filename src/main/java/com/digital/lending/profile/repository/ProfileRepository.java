package com.digital.lending.profile.repository;

import com.digital.lending.profile.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, String> {

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(i) > 0 FROM IndividualProfile p JOIN p.identities i WHERE i.documentNumber = :docNum")
    boolean existsInIndividualIdentities(@Param("docNum") String docNum);

    @Query("SELECT COUNT(d) > 0 FROM CorporateProfile p JOIN p.directorIdentities d WHERE d.documentNumber = :docNum")
    boolean existsInCorporateIdentities(@Param("docNum") String docNum);

    @Query("SELECT COUNT(a) > 0 FROM JointProfile p JOIN p.applicantIdentities a WHERE a.documentNumber = :docNum")
    boolean existsInJointIdentities(@Param("docNum") String docNum);
}
