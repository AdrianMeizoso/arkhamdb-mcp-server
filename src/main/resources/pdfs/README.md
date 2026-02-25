# PDF Resources Directory

This directory contains PDF documents for Arkham Horror: The Card Game that are exposed through the MCP server.

## Base Game Files

Place the following PDF files in this directory:

1. **arkham_horror_rules.pdf**
   - Official rules reference for Arkham Horror: The Card Game
   - Available from: Fantasy Flight Games website or the Learn to Play/Rules Reference PDFs
   - Resource URI: `arkhamdb://rules/pdf`

2. **arkham_horror_faq.pdf**
   - Official FAQ and rules clarifications
   - Available from: Fantasy Flight Games website
   - Resource URI: `arkhamdb://rules/faq`

## Campaign Guide Files

Campaign guide PDFs enable the `get_campaign_rules` tool. Use the exact filenames below:

| Filename | Campaign |
|----------|----------|
| `campaign_core_guide.pdf` | Night of the Zealot (core) |
| `campaign_dwl_guide.pdf` | The Dunwich Legacy (dwl) |
| `campaign_ptc_guide.pdf` | The Path to Carcosa (ptc) |
| `campaign_tfa_guide.pdf` | The Forgotten Age (tfa) |
| `campaign_tcu_guide.pdf` | The Circle Undone (tcu) |
| `campaign_tde_guide.pdf` | The Dream-Eaters (tde) |
| `campaign_tic_guide.pdf` | The Innsmouth Conspiracy (tic) |
| `campaign_eoe_guide.pdf` | Edge of the Earth (eoe) |
| `campaign_tsk_guide.pdf` | The Scarlet Keys (tsk) |
| `campaign_fhv_guide.pdf` | The Feast of Hemlock Vale (fhv) |

Campaign PDFs are **optional** — if a PDF is missing, the tool will still return encounter card data from the API and inform the user of the expected filename.

## How to Use

1. Download the official PDFs from Fantasy Flight Games / Asmodee
2. Rename them to match the filenames above
3. Place them in this directory: `src/main/resources/pdfs/`
4. Rebuild the project: `./gradlew shadowJar`
5. Restart Claude Desktop

## Notes

- All PDFs are cached in memory after first access (no re-reads on subsequent calls)
- The PDFs will be embedded in the JAR file when you build with `shadowJar`
- Text is automatically extracted from the PDFs when accessed
- Campaign PDFs are gracefully skipped if not present; API data is always available
