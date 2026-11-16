<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="${s.moduleCode} attendance" active="sessions" wide="true">
    <header class="page-head">
        <p class="muted"><c:out value="${s.dateText}, ${s.roomName}"/></p>
        <h1><c:out value="${s.moduleCode} ${s.moduleName}"/></h1>
        <p class="lead" id="live-summary" aria-live="polite"><c:out value="${inCount} of ${s.enrolled} checked in"/><c:if test="${flaggedCount > 0}"><c:out value=", ${flaggedCount} to review"/></c:if>.</p>
        <div class="page-actions">
            <a class="button button-quiet" href="${ctx}/lecturer/roster.csv?id=${s.id}"><svg class="ico" aria-hidden="true" focusable="false"><use href="#i-download"/></svg>Download (CSV)</a>
        </div>
    </header>

    <c:choose>
        <c:when test="${open}">
            <section class="live" data-live="${ctx}/lecturer/live?id=${s.id}" data-qr="${ctx}/lecturer/qr?id=${s.id}" aria-labelledby="code-title">
                <div class="live-code">
                    <h2 id="code-title">Code on the screen</h2>
                    <p class="code" data-code><c:out value="${code}"/></p>
                    <p class="muted"><span data-seconds><c:out value="${secondsLeft}"/></span> seconds until it changes. Check-in closes at <span data-closes><c:out value="${s.closesText}"/></span>.</p>
                    <div class="page-actions">
                        <a class="button" href="${ctx}/lecturer/display?id=${s.id}" target="_blank" rel="noopener">Show on projector</a>
                        <form action="${ctx}/lecturer/extend" method="post" class="inline-form">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${s.id}">
                            <button class="button button-quiet" type="submit">10 more minutes</button>
                        </form>
                        <form action="${ctx}/lecturer/close" method="post" class="inline-form">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${s.id}">
                            <button class="button button-quiet" type="submit">Close check-in</button>
                        </form>
                    </div>
                </div>
                <img class="qr" data-qr-img src="${ctx}/lecturer/qr?id=${s.id}" alt="QR code for students to scan to check in" width="240" height="240">
                <noscript><p class="muted">Reload the page to see the newest code.</p></noscript>
            </section>
        </c:when>
        <c:otherwise>
            <div class="notice"><p><strong>Check-in is closed.</strong>&#32;You can still correct any student’s record below.</p>
                <form action="${ctx}/lecturer/extend" method="post" class="inline-form">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${s.id}">
                    <button class="button button-quiet" type="submit">Reopen for 10 minutes</button>
                </form></div>
        </c:otherwise>
    </c:choose>

    <section aria-labelledby="roster-title">
        <h2 id="roster-title">Class list</h2>
        <p class="muted small">Flagged check-ins are listed first. Confidence combines the code or QR, the campus network and the device; see Help.</p>
        <div class="table-wrap">
            <table class="table roster">
                <thead><tr><th scope="col">Student</th><th scope="col">Status</th><th scope="col">Time</th><th scope="col">How</th><th scope="col">Confidence</th><th scope="col">Change record</th></tr></thead>
                <tbody>
                <c:forEach var="r" items="${roster}">
                    <c:set var="ci" value="${r.checkIn}"/>
                    <tr class="${not empty ci and ci.flagged ? 'row-flag' : ''}">
                        <th scope="row"><c:out value="${r.student.fullName}"/><br><span class="muted small"><c:out value="${r.student.studentNumber}"/></span></th>
                        <td><c:out value="${r.statusLabel}"/>
                            <c:if test="${not empty ci and ci.flagged}"><br><span class="pill pill-dark"><svg class="ico ico-s" aria-hidden="true" focusable="false"><use href="#i-flag"/></svg>Review</span></c:if>
                            <c:if test="${not empty ci and ci.corrected}"><br><span class="muted small"><c:out value="${ci.correctionReason} (${ci.correctedByName})"/></span></c:if></td>
                        <td><c:out value="${empty ci ? '' : ci.timeText}"/></td>
                        <td><c:out value="${empty ci ? '' : ci.methodLabel}"/></td>
                        <td><c:if test="${not empty ci}"><c:out value="${ci.confidenceLabel} (${ci.confidence})"/><br><span class="muted small"><c:out value="${ci.factors}"/></span></c:if></td>
                        <td>
                            <c:if test="${not empty ci and ci.flagged}">
                                <form action="${ctx}/lecturer/accept" method="post" class="inline-form">
                                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${s.id}"><input type="hidden" name="student" value="${r.student.id}">
                                    <button class="button button-small" type="submit">Accept</button>
                                </form>
                            </c:if>
                            <form action="${ctx}/lecturer/correct" method="post" class="correct-form">
                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${s.id}"><input type="hidden" name="student" value="${r.student.id}">
                                <label class="visually-hidden" for="st-${r.student.id}"><c:out value="New status for ${r.student.fullName}"/></label>
                                <select class="control control-small" id="st-${r.student.id}" name="status">
                                    <c:forEach var="st" items="${statuses}"><option value="${st.name}" ${not empty ci and ci.status == st ? 'selected' : ''}><c:out value="${st.label}"/></option></c:forEach>
                                </select>
                                <label class="visually-hidden" for="re-${r.student.id}"><c:out value="Reason for ${r.student.fullName}"/></label>
                                <input class="control control-small" id="re-${r.student.id}" name="reason" placeholder="Reason" required minlength="3" maxlength="300">
                                <button class="button button-small button-quiet" type="submit">Save</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </section>
    <script src="${ctx}/assets/js/live.js" defer></script>
</s:shell>
