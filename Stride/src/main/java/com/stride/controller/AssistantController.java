package com.stride.controller;

import com.stride.config.App;
import com.stride.model.User;
import com.stride.service.AssistantService;
import com.stride.util.SessionKeys;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The study assistant (RQ25): a plain form, so it works without JavaScript. */
@WebServlet(name = "assistantController", urlPatterns = "/assistant")
public class AssistantController extends BaseController {

    /** One turn of the conversation kept in the session (last ten only). */
    public static final class Turn implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String question;
        private final List<String> answer;
        private final String source;
        private final List<String> suggestions;

        Turn(String question, AssistantService.Reply reply) {
            this.question = question;
            this.answer = new ArrayList<>(reply.getLines());
            this.source = reply.getSource();
            this.suggestions = new ArrayList<>(reply.getSuggestions());
        }

        public String getQuestion() { return question; }
        public List<String> getAnswer() { return answer; }
        public String getSource() { return source; }
        public List<String> getSuggestions() { return suggestions; }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("chat", chat(request.getSession()));
        request.setAttribute("starters", AssistantService.STARTERS);
        render(request, response, "assistant");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        List<Turn> chat = chat(session);
        if ("clear".equals(request.getParameter("action"))) {
            chat.clear();
        } else {
            String q = param(request, "q");
            if (!q.isEmpty()) {
                if (q.length() > 300) q = q.substring(0, 300);
                chat.add(new Turn(q, app().assistant.answer(q, context(app(), user(request)))));
                while (chat.size() > 10) chat.remove(0);
            }
        }
        session.setAttribute(SessionKeys.CHAT, chat);
        redirect(request, response, "/assistant#latest");
    }

    /** Gathers the student's own data for the assistant to answer from. */
    public static AssistantService.Context context(App app, User user) {
        LocalDateTime now = app.now();
        return new AssistantService.Context(user, now, app.schedule.entriesFor(user),
                app.attendance.summaryFor(user, app.curriculum.modulesFor(user)),
                app.planner.upcoming(user, now, 21), app.announcements.feed(user),
                app.schedule.freeRoomsNow(now), app.curriculum.buildings(), app.curriculum.modulesFor(user),
                app.riskPercent);
    }

    @SuppressWarnings("unchecked")
    private static List<Turn> chat(HttpSession session) {
        Object v = session.getAttribute(SessionKeys.CHAT);
        return v instanceof List ? (List<Turn>) v : new ArrayList<>();
    }
}
