export async function GET() {
    return Response.json(
        { status: "ok", service: "wealthix-ui" },
        { headers: { "Cache-Control": "no-store" } }
    );
}
