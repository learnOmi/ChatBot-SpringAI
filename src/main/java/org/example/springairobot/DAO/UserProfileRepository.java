package org.example.springairobot.DAO;

import org.example.springairobot.PO.Tables.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
}