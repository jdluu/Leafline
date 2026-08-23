# Leafline

Leafline is an Android-first EPUB reader with local library support and OPDS
integration. It is designed for focused reading on Android and e-ink devices,
with Grimmory as the first supported OPDS catalog.

For the strict division of responsibility between Leafline (reader) and
ShelfSync (Grimmory sync client), see docs/app-boundaries.md.

## Current status

The project is under active development. The current prototype can:

- Render EPUB 2 and EPUB 3 books with the Readium Kotlin Toolkit
- Import local EPUB files into a Room-backed library
- Show library books in a grid with cached cover thumbnails
- Sort the library by recent, title, or author (choice is remembered)
- Browse an authenticated OPDS 1.2 catalog
- Navigate OPDS feeds and acquisition entries
- Download EPUB acquisitions into the local library
- Open imported books for reading offline
- Bookmark reading positions and jump back to them
- Search inside the open book and jump to matches

The reading experience, settings persistence, search, progress synchronization,
and release packaging are still being developed.

## Architecture

Leafline is a native Kotlin Android application using:

- Jetpack Compose and Material 3 for the interface
- Readium Kotlin Toolkit for EPUB handling and rendering
- Room for local metadata and library state
- OPDS 1.2 as the library integration boundary
- Kotlin coroutines and Flow for asynchronous work

The app keeps the reading experience and local library on-device. Grimmory and
other compatible services remain external catalog/library providers.

## Building

Requirements:

- Android SDK
- JDK 17 or newer
- Gradle (the repository includes a wrapper)

Run the standard checks from the repository root:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Do not commit `local.properties`, signing material, generated APKs, credentials,
or other machine-specific files.

## OPDS configuration

Leafline accepts an OPDS catalog URL and HTTP Basic Authentication credentials
through the app interface. Credentials are held in memory in the current
prototype and are not committed to this repository.

For Grimmory, use the OPDS endpoint exposed by your own server installation.
The server must have OPDS access enabled and the account must have permission to
read the desired libraries.

## Project documentation

- [Architecture notes](docs/architecture.md)
- [OPDS integration plan](docs/opds-plan.md)
- [Local library plan](docs/local-library-plan.md)
- [Contributing](CONTRIBUTING.md) (when available)

These documents describe implementation details and may change as the project
matures.

## License

No license has been selected yet. Until a license is added, all rights remain
reserved.

## Name

Leafline refers to both the leaves of a book and a continuous reading line
across devices.

## References

- [Readium Kotlin Toolkit](https://readium.org/kotlin-toolkit)
- [Grimmory](https://grimmory.org/)
- [OPDS specification](https://specs.opds.io/)
