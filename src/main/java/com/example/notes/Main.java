package com.example.notes;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.SimpleFileServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    // In-memory notes (not thread-safe; for demo only)
    private static final List<Note> notes = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // Create a simple HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // 1) Serve static files from "static" folder at the root URL ("/")
        server.createContext("/", SimpleFileServer.createFileHandler(Path.of("/static")));

        // 2) API context: /api/notes
        server.createContext("/api/notes", Main::handleNotes);

        server.start();
        System.out.println("Server is running on http://localhost:8080");
    }

    /**
     * Main handler for the /api/notes context.
     * We’ll parse the path to see if it's:
     *   - GET /api/notes
     *   - POST /api/notes
     *   - POST /api/notes/{id}/vote?type=up|down
     */
    private static void handleNotes(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();    // e.g. "/api/notes/3/vote"
            String query = exchange.getRequestURI().getQuery();  // e.g. "type=up"

            // Because we mapped "/api/notes" at the context level,
            // path beyond that is what's after "/api/notes".
            // For example, if the full path is "/api/notes/3/vote", then the "remaining" is "/3/vote"
            // but we can just parse the entire path and do a quick check.

            // 1) If it's exactly "/api/notes" with no extra, handle GET/POST of the notes list
            if ("/api/notes".equals(path)) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetAllNotes(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleCreateNote(exchange);
                } else {
                    sendStatus(exchange, 405); // Method Not Allowed
                }
                return;
            }

            // 2) Check if it looks like "/api/notes/{id}/vote"
            // We'll do a quick parse:
            //   path = "/api/notes/3/vote" => Split on "/" => ["", "api", "notes", "3", "vote"]
            String[] parts = path.split("/");
            // We expect parts[0] = "", parts[1] = "api", parts[2] = "notes", parts[3] = noteId, parts[4] = "vote"
            if (parts.length == 5 && "vote".equalsIgnoreCase(parts[4]) && "POST".equalsIgnoreCase(method)) {
                int noteId = Integer.parseInt(parts[3]);  // might throw if not an integer
                handleVoteOnNote(exchange, noteId, query);
                return;
            }

            if (parts.length == 4 && "DELETE".equalsIgnoreCase(method)) {
                int noteId = Integer.parseInt(parts[3]);  // might throw if not an integer
                handleDeleteNote(exchange, noteId);
                return;
            }

            // If we get here, path didn't match anything we handle
            sendStatus(exchange, 404);
        } catch (Exception e) {
            e.printStackTrace();
            sendStatus(exchange, 500); // Internal Server Error
        }
    }

    private static void handleGetAllNotes(HttpExchange exchange) throws IOException {
        JSONArray result = new JSONArray();
        for (Note n : notes) {
            result.add(n.getJSONObject());
        }
        String json = result.toJSONString();
        byte[] bytes = json.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void handleCreateNote(HttpExchange exchange) throws IOException {
        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        String body = new String(bodyBytes);

        // We'll parse the incoming JSON, expecting { "text": "some note text" }
        JSONObject incoming = (JSONObject) JSONValue.parse(body);
        Note newNote = new Note((String) incoming.get("text"));
        notes.add(newNote);

        String json = newNote.getJSONObject().toJSONString();
        byte[] bytes = json.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Handles DELETE /api/notes/{id}
     */
    private static void handleDeleteNote(HttpExchange exchange, int noteId) throws IOException {
        for (int i = 0; i < notes.size(); i++) {
            Note n = notes.get(i);
            if (n.getId() == noteId) {
                notes.remove(i);
                sendStatus(exchange, 204);
                return;
            }
        }
        sendStatus(exchange, 500);
    }

    /**
     * Handles POST /api/notes/{id}/vote?type=up|down
     */
    private static void handleVoteOnNote(HttpExchange exchange, int noteId, String query) throws IOException {
        // Find the note by ID
        Note note = findNoteById(noteId);
        if (note == null) {
            sendStatus(exchange, 404);
            return;
        }

        // Parse query param "type=up" or "type=down"
        // We'll do a simple approach. For a real app you might do something more robust.
        String voteType = null;
        if (query != null) {
            // e.g. query = "type=up"
            // naive parse:
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2 && "type".equalsIgnoreCase(kv[0])) {
                    voteType = kv[1];
                }
            }
        }

        if ("up".equalsIgnoreCase(voteType)) {
            note.upvote();
        } else if ("down".equalsIgnoreCase(voteType)) {
            note.downvote();
        } else {
            // 400 = Bad Request
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        // Return the updated note as JSON
        String json = note.getJSONObject().toJSONString();
        byte[] bytes = json.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Note findNoteById(int id) {
        for (Note n : notes) {
            if (n.getId() == id) {
                return n;
            }
        }
        return null;
    }

    private static void sendStatus(HttpExchange exchange, int code) throws IOException {
        exchange.sendResponseHeaders(code, -1);
        exchange.close();
    }
}