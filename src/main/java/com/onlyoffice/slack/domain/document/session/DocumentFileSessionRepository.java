package com.onlyoffice.slack.domain.document.session;

import com.onlyoffice.slack.domain.document.session.entity.ActiveFileSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentFileSessionRepository extends JpaRepository<ActiveFileSession, String> {}
