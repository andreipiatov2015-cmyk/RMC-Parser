package com.rmc.parser.model;

import java.util.Optional;

/**
 * Заявка на соглашение (страница /cooperates/ на сайте).
 *
 * <p>Структура похожа на {@link Program}, но это другой раздел сайта:
 * заявка связывает "уполномоченный орган" (плательщика) и учреждение на
 * определённый период, и имеет статус (новая / подтверждена / отклонена).</p>
 */
public class Cooperate {

    /**
     * Статус заявки на соглашение.
     */
    public enum Status {
        NEW,
        CONFIRMED,
        REJECTED,
        UNKNOWN
    }

    private final String id;
    private final String date;
    private final String payerName;
    private final String payerUrl;
    private final String payerTerritory;
    private final String schoolName;
    private final String schoolUrl;
    private final String schoolTerritory;
    private final String schoolMinistry;
    private final String period;
    private final Status status;
    private final String statusText;
    private final String maxHours;
    private final String confirmedHours;

    private Cooperate(Builder builder) {
        this.id = builder.id;
        this.date = builder.date;
        this.payerName = builder.payerName;
        this.payerUrl = builder.payerUrl;
        this.payerTerritory = builder.payerTerritory;
        this.schoolName = builder.schoolName;
        this.schoolUrl = builder.schoolUrl;
        this.schoolTerritory = builder.schoolTerritory;
        this.schoolMinistry = builder.schoolMinistry;
        this.period = builder.period;
        this.status = builder.status != null ? builder.status : Status.UNKNOWN;
        this.statusText = builder.statusText;
        this.maxHours = builder.maxHours;
        this.confirmedHours = builder.confirmedHours;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public Optional<String> getDate() {
        return Optional.ofNullable(date);
    }

    public Optional<String> getPayerName() {
        return Optional.ofNullable(payerName);
    }

    public Optional<String> getPayerUrl() {
        return Optional.ofNullable(payerUrl);
    }

    public Optional<String> getPayerTerritory() {
        return Optional.ofNullable(payerTerritory);
    }

    public Optional<String> getSchoolName() {
        return Optional.ofNullable(schoolName);
    }

    public Optional<String> getSchoolUrl() {
        return Optional.ofNullable(schoolUrl);
    }

    public Optional<String> getSchoolTerritory() {
        return Optional.ofNullable(schoolTerritory);
    }

    public Optional<String> getSchoolMinistry() {
        return Optional.ofNullable(schoolMinistry);
    }

    public Optional<String> getPeriod() {
        return Optional.ofNullable(period);
    }

    public Status getStatus() {
        return status;
    }

    public Optional<String> getStatusText() {
        return Optional.ofNullable(statusText);
    }

    public Optional<String> getMaxHours() {
        return Optional.ofNullable(maxHours);
    }

    public Optional<String> getConfirmedHours() {
        return Optional.ofNullable(confirmedHours);
    }

    @Override
    public String toString() {
        return "Cooperate{" +
                "id='" + id + '\'' +
                ", schoolName='" + schoolName + '\'' +
                ", status=" + status +
                '}';
    }

    public static class Builder {

        private String id;
        private String date;
        private String payerName;
        private String payerUrl;
        private String payerTerritory;
        private String schoolName;
        private String schoolUrl;
        private String schoolTerritory;
        private String schoolMinistry;
        private String period;
        private Status status;
        private String statusText;
        private String maxHours;
        private String confirmedHours;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder date(String date) {
            this.date = date;
            return this;
        }

        public Builder payerName(String payerName) {
            this.payerName = payerName;
            return this;
        }

        public Builder payerUrl(String payerUrl) {
            this.payerUrl = payerUrl;
            return this;
        }

        public Builder payerTerritory(String payerTerritory) {
            this.payerTerritory = payerTerritory;
            return this;
        }

        public Builder schoolName(String schoolName) {
            this.schoolName = schoolName;
            return this;
        }

        public Builder schoolUrl(String schoolUrl) {
            this.schoolUrl = schoolUrl;
            return this;
        }

        public Builder schoolTerritory(String schoolTerritory) {
            this.schoolTerritory = schoolTerritory;
            return this;
        }

        public Builder schoolMinistry(String schoolMinistry) {
            this.schoolMinistry = schoolMinistry;
            return this;
        }

        public Builder period(String period) {
            this.period = period;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder statusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        public Builder maxHours(String maxHours) {
            this.maxHours = maxHours;
            return this;
        }

        public Builder confirmedHours(String confirmedHours) {
            this.confirmedHours = confirmedHours;
            return this;
        }

        public Cooperate build() {
            return new Cooperate(this);
        }
    }
}
