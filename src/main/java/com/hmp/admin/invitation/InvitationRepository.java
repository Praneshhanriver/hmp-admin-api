package com.hmp.admin.invitation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvitationRepository extends JpaRepository<Invitation, Long>, JpaSpecificationExecutor<Invitation> {

	// Detail screen: load the history in the same query
	@EntityGraph(attributePaths = "history")
	Optional<Invitation> findWithHistoryById(Long id);

	// Duplicate checks for Issue / Edit (W-02d already invited, W-02e already registered)
	boolean existsByEmailAndStatus(String email, InvitationStatus status);

	boolean existsByEmailAndStatusAndIdNot(String email, InvitationStatus status, Long id);

	// Expiry job: Pending invitations whose link is past its expiry
	List<Invitation> findByStatusAndExpiresAtLessThanEqual(InvitationStatus status, Instant now);

}
