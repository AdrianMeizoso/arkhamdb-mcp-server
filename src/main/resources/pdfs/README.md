# PDF Resources Directory

This directory contains PDF documents for Arkham Horror: The Card Game that are exposed through the MCP server.

## Required Files

Place the following PDF files in this directory:

1. **arkham_horror_rules.pdf**
   - Official rules reference for Arkham Horror: The Card Game
   - Available from: Fantasy Flight Games website or the Learn to Play/Rules Reference PDFs
   - Resource URI: `arkhamdb://rules/pdf`

2. **arkham_horror_faq.pdf**
   - Official FAQ and rules clarifications
   - Available from: Fantasy Flight Games website
   - Resource URI: `arkhamdb://rules/faq`

## How to Use

1. Download the official PDFs from Fantasy Flight Games
2. Rename them to match the filenames above
3. Place them in this directory: `src/main/resources/pdfs/`
4. Rebuild the project: `./gradlew shadowJar`
5. Restart Claude Desktop

## Notes

- The PDFs will be embedded in the JAR file when you build with `shadowJar`
- Text is automatically extracted from the PDFs when accessed
- The resources are available to Claude for answering rules and FAQ questions
