package com.clara.challenge.events;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivedEventRepository extends JpaRepository<ReceivedEvent, String> {}
