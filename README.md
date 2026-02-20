# ArkhamDB MCP Server

A Model Context Protocol (MCP) server that provides AI assistants like Claude with access to the [ArkhamDB API](https://arkhamdb.com/api/) for the Arkham Horror Living Card Game.

## Features

- **Card Search**: Search and filter cards by name, faction, type, and traits
- **Card Details**: Get detailed information about specific cards
- **Pack Listing**: List all available card packs and expansions
- **Decklist Access**: Retrieve public decklists from ArkhamDB
- **HTTP Caching**: Respects API cache headers for improved performance
- **MCP Resources**: Provides passive access to card database for AI context

## Prerequisites

- Java 21 or higher
- Gradle (included via wrapper)
- Claude Desktop or compatible MCP client

## Building

Build the project and create a fat JAR:

```bash
./gradlew shadowJar
```

The executable JAR will be created at `build/libs/arkhamdb-mcp-server-1.0.0-all.jar`

## Installation

### Claude Desktop

Add the server to your Claude Desktop configuration file:

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`

**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

**Linux**: `~/.config/Claude/claude_desktop_config.json`

Configuration:

```json
{
  "mcpServers": {
    "arkhamdb": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/arkhamdb-mcp-server-1.0.0-all.jar"
      ]
    }
  }
}
```

Replace `/absolute/path/to/` with the actual path to your built JAR file.

### Restart Claude Desktop

After updating the configuration, restart Claude Desktop to load the server.

## Available Tools

### search_cards

Search for Arkham Horror LCG cards with optional filters.

**Parameters:**
- `name` (optional): Card name substring search (case-insensitive)
- `faction` (optional): Filter by faction (guardian, seeker, rogue, mystic, survivor, neutral)
- `type` (optional): Card type (asset, event, skill, investigator, etc.)
- `traits` (optional): Card traits (weapon, spell, ally, etc.)

**Example queries:**
- "Show me all Guardian cards"
- "Find cards with the Weapon trait"
- "Search for cards named 'Roland'"

### get_card

Get detailed information about a specific card by its code.

**Parameters:**
- `code` (required): Card code (e.g., "01001" for Roland Banks)

**Example queries:**
- "What does card 01001 do?"
- "Get details for card code 02110"

### list_packs

List all available card packs and expansions.

**Example queries:**
- "List all card packs"
- "What expansions are available?"

### get_decklist

Retrieve a public decklist by its ID.

**Parameters:**
- `id` (required): Decklist ID number

**Example queries:**
- "Show me decklist 123"
- "Get the details for decklist 456"

## Available Resources

Resources provide passive context that AI assistants can reference without explicit tool calls.

### arkhamdb://cards

Complete card database for Arkham Horror LCG. Contains all cards with full details.

### arkhamdb://packs

List of all card packs and expansions with metadata.

### arkhamdb://card/{code}

Individual card details by card code. This is a resource template that can be used with any card code.

**Example**: `arkhamdb://card/01001` for Roland Banks

### arkhamdb://rules/pdf

Complete rules reference document for Arkham Horror: The Card Game extracted from the official rules PDF. Provides the full rules text for AI assistants to reference when answering gameplay questions.

### arkhamdb://rules/faq

Official FAQ and rules clarifications for Arkham Horror: The Card Game. Contains frequently asked questions, edge case rulings, and official errata for cards and game mechanics.

## Usage Examples

Once installed in Claude Desktop, you can ask questions like:

**Card Database Queries:**
- "What are the best Guardian cards for dealing damage?"
- "Show me all Mystic spell cards that cost 2 or less"
- "What cards are in the Dunwich Legacy expansion?"
- "Find cards with the Relic trait"
- "What does Roland Banks do?"
- "Show me popular investigator decklists"

**Rules and FAQ Queries:**
- "How does the mythos phase work?"
- "What are the timing rules for Fast cards?"
- "Can I trigger a reaction during an attack of opportunity?"
- "What happens when I draw the encounter deck empty?"
- "How do Victory points work?"

## Development

### Project Structure

```
src/main/kotlin/com/arkhamdb/mcp/
├── Main.kt                    # Server entry point
├── ArkhamDbClient.kt          # HTTP client with caching
├── models/                    # Data models
│   ├── Card.kt
│   ├── Pack.kt
│   └── Decklist.kt
├── tools/                     # MCP tool implementations
│   ├── CardTools.kt
│   ├── PackTools.kt
│   └── DecklistTools.kt
└── resources/                 # MCP resource providers
    └── ArkhamDbResources.kt
```

### Running in Development

```bash
./gradlew run
```

### Testing with MCP Inspector

You can test the server using the [MCP Inspector](https://github.com/modelcontextprotocol/inspector):

```bash
npx @modelcontextprotocol/inspector java -jar build/libs/arkhamdb-mcp-server-1.0.0-all.jar
```

### Logging

All logs are sent to stderr to avoid interfering with the MCP protocol on stdout. Log level can be adjusted in `src/main/resources/logback.xml`.

## Technical Details

### HTTP Caching

The server uses Ktor's built-in HTTP caching to respect `Cache-Control` and `ETag` headers from the ArkhamDB API. This:
- Reduces load on ArkhamDB servers
- Improves response times for repeated queries
- Complies with API usage guidelines

### MCP Protocol

This server implements the Model Context Protocol using the official Kotlin SDK. It supports:
- **Tools**: Active operations that can be invoked by the AI
- **Resources**: Passive data that can be referenced for context
- **STDIO Transport**: Communication via standard input/output

### API Information

The server uses the [ArkhamDB Public API](https://arkhamdb.com/api/):
- Base URL: `https://arkhamdb.com/api/public/`
- No authentication required for public endpoints
- Rate limiting is handled through HTTP caching

## Troubleshooting

### Server won't start

- Check that Java 11+ is installed: `java -version`
- Verify the JAR file exists at the specified path
- Check Claude Desktop logs for error messages

### No data returned

- Verify internet connectivity
- Check that ArkhamDB.com is accessible
- Look at stderr output for error messages

### Connection errors

- Ensure no firewall is blocking outbound HTTPS connections
- Verify the server process is running

## License

This project is a community tool for Arkham Horror LCG players and is not officially affiliated with Fantasy Flight Games or ArkhamDB.

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## Resources

- [ArkhamDB](https://arkhamdb.com/) - Official card database
- [ArkhamDB API Documentation](https://arkhamdb.com/api/)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk)
