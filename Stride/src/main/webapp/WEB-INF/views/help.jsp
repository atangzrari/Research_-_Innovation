<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Help" active="help">
    <header class="page-head">
        <h1>Help</h1>
        <p class="lead">Stride puts the college’s websites, your classes and your attendance in one place. Here is how to use it.</p>
    </header>

    <section aria-labelledby="steps-title">
        <h2 id="steps-title">Three steps</h2>
        <ol class="steps">
            <li><strong>Go to Home.</strong> Press <em>Home</em> on the left (or at the bottom of a phone).</li>
            <li><strong>Press the big button for what you want.</strong> For example, <em>Check my results</em>.</li>
            <li><strong>A new tab opens.</strong> Sign in there with your college account. Come back to Stride by clicking its tab.</li>
        </ol>
        <a class="button" href="${ctx}/dashboard">Go to Home</a>
    </section>

    <section aria-labelledby="faq-title">
        <h2 id="faq-title">Common questions</h2>
        <details>
            <summary>How do I check in to a class?</summary>
            <p>When your lecturer opens attendance, a QR image and a six-character code appear on the screen. Scan the QR with your phone’s camera, or open <em>Check in</em> and type the code. The code changes every few seconds, so a photo of it sent to someone else will not work.</p>
        </details>
        <details>
            <summary>Why does my check-in say “under review”?</summary>
            <p>Stride checks more than the code: for example whether you are on the college network, and whether your phone was already used by another student for the same class. If the signals are weak, your lecturer looks at the check-in and accepts or changes it.</p>
        </details>
        <details>
            <summary>What is the study planner?</summary>
            <p>It looks at your deadlines and your timetable, and suggests hours to study for each piece of work, spread out before it is due. You can add the plan to your phone’s calendar.</p>
        </details>
        <details>
            <summary>What can the assistant answer?</summary>
            <p>Questions about your own Stride information: your next class, your attendance, your deadlines, notices, and free rooms. It does not make answers up: if it cannot answer from your data, it says so.</p>
        </details>
        <details>
            <summary>The writing is too small.</summary>
            <p>At the top of every page there are three buttons with the letter A. Press the middle one for large text, or the right one for extra large text.</p>
        </details>
        <details>
            <summary>Why do I have to sign in again on another site?</summary>
            <p>Stride opens the college’s own websites. Each one keeps its own sign-in, so use the same details you always use for that site.</p>
        </details>
        <details>
            <summary>How do I find something quickly?</summary>
            <p>Type a word, such as <em>results</em>, <em>books</em> or a module code like <em>CSE305</em>, in the box at the top and press Search. Press the <kbd>/</kbd> key to jump to the box.</p>
        </details>
        <details>
            <summary>How do I leave Stride safely?</summary>
            <p>Press <em>Sign out</em> at the top right. Always do this on a shared computer.</p>
        </details>
        <details>
            <summary>I still need help.</summary>
            <p>Ask ICT Support in the Main building.</p>
        </details>
    </section>
</s:shell>
