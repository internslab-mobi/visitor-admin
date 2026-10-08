package com.adminvisitor.specification;

import com.adminvisitor.entity.Visit;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitView;
import com.adminvisitor.enums.VisitorType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class VisitSpecification {

    private VisitSpecification() {
    }

    public static Specification<Visit> hasView(VisitView view) {

        if (view == null) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {

            LocalDate today = LocalDate.now();

            LocalDateTime startOfToday =
                    today.atStartOfDay();

            LocalDateTime startOfTomorrow =
                    today.plusDays(1).atStartOfDay();

            switch (view) {

                case TODAY:

                    return criteriaBuilder.and(
                            criteriaBuilder.greaterThanOrEqualTo(
                                    root.get("expectedArrivalAt"),
                                    startOfToday
                            ),
                            criteriaBuilder.lessThan(
                                    root.get("expectedArrivalAt"),
                                    startOfTomorrow
                            )
                    );

                case FUTURE:

                    return criteriaBuilder.greaterThanOrEqualTo(
                            root.get("expectedArrivalAt"),
                            startOfTomorrow
                    );

                case COMPLETED:

                    return criteriaBuilder.equal(
                            root.get("status"),
                            VisitStatus.CHECKED_OUT
                    );

                default:

                    return criteriaBuilder.conjunction();
            }
        };
    }

    public static Specification<Visit> hasVisitorId(String visitorId) {

        if (visitorId == null) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {

            Join<Object, Object> visitor =
                    root.join("visitor", JoinType.INNER);

            return criteriaBuilder.equal(
                    visitor.get("id"),
                    visitorId
            );
        };
    }

    public static Specification<Visit> hasVisitorName(String visitorName) {

        if (visitorName == null || visitorName.isBlank()) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {

            Join<Object, Object> visitor =
                    root.join("visitor", JoinType.INNER);

            String searchValue =
                    "%" + visitorName.trim().toLowerCase() + "%";

            return criteriaBuilder.or(

                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    visitor.get("firstName")
                            ),
                            searchValue
                    ),

                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    visitor.get("lastName")
                            ),
                            searchValue
                    ),

                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    criteriaBuilder.concat(
                                            criteriaBuilder.concat(
                                                    visitor.get("firstName"),
                                                    " "
                                            ),
                                            visitor.get("lastName")
                                    )
                            ),
                            searchValue
                    )
            );
        };
    }

    public static Specification<Visit> hasVisitorEmail(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {

            Join<Object, Object> visitor =
                    root.join("visitor", JoinType.INNER);

            return criteriaBuilder.like(
                    criteriaBuilder.lower(
                            visitor.get("email")
                    ),
                    "%" + email.trim().toLowerCase() + "%"
            );
        };
    }

    public static Specification<Visit> hasStatus(VisitStatus status) {

        if (status == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<Visit> hasDate(LocalDate date) {

        if (date == null) {
            return null;
        }

        LocalDateTime start =
                date.atStartOfDay();

        LocalDateTime end =
                date.plusDays(1).atStartOfDay();

        return (root, query, criteriaBuilder) ->

                criteriaBuilder.and(

                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("expectedArrivalAt"),
                                start
                        ),

                        criteriaBuilder.lessThan(
                                root.get("expectedArrivalAt"),
                                end
                        )
                );
    }

    public static Specification<Visit> hasFromDate(LocalDate fromDate) {

        if (fromDate == null) {
            return null;
        }

        LocalDateTime start =
                fromDate.atStartOfDay();

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("expectedArrivalAt"),
                        start
                );
    }

    public static Specification<Visit> hasToDate(LocalDate toDate) {

        if (toDate == null) {
            return null;
        }

        LocalDateTime end =
                toDate.plusDays(1).atStartOfDay();

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(
                        root.get("expectedArrivalAt"),
                        end
                );
    }

    public static Specification<Visit> hasSearch(String search) {

        if (search == null || search.isBlank()) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {

            Join<Object, Object> visitor =
                    root.join("visitor", JoinType.INNER);

            Join<Object, Object> host =
                    root.join("host", JoinType.INNER);

            String searchValue =
                    "%" + search.trim().toLowerCase() + "%";

            return criteriaBuilder.or(

                    // Visit reference
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("visitReference")
                            ),
                            searchValue
                    ),

                    // Visitor ID
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    visitor.get("id")
                            ),
                            searchValue
                    ),

                    // Visitor first name
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    visitor.get("firstName")
                            ),
                            searchValue
                    ),

                    // Visitor last name
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    visitor.get("lastName")
                            ),
                            searchValue
                    ),

                    // Visitor email
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    visitor.get("email")
                            ),
                            searchValue
                    ),

                    // Visitor mobile
                    criteriaBuilder.like(
                            visitor.get("mobileNumber"),
                            searchValue
                    ),

                    // Host first name
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    host.get("firstName")
                            ),
                            searchValue
                    ),

                    // Host last name
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    host.get("lastName")
                            ),
                            searchValue
                    )
            );
        };
    }

    public static Specification<Visit> hasVisitorType(VisitorType visitorType) {
        if (visitorType == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("visitorType"),
                        visitorType
                );
    }

    public static Specification<Visit> hasHostId(String hostId) {
        if (hostId == null || hostId.isBlank()) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {
            Join<Object, Object> host =
                    root.join("host", JoinType.INNER);

            return criteriaBuilder.equal(
                    host.get("id"),
                    hostId.trim()
            );
        };
    }


}