package com.cookiejar.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "disabled_dates", uniqueConstraints = @UniqueConstraint(columnNames = "date"))
public class DisabledDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    private String reason;

    @Column(name = "popup_message", columnDefinition = "TEXT")
    private String popupMessage;

    public DisabledDate() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPopupMessage() { return popupMessage; }
    public void setPopupMessage(String popupMessage) { this.popupMessage = popupMessage; }
}
