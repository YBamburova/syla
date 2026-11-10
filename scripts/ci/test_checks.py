"""Regression coverage for checks whose failures enforce the merge gate."""

import tempfile
import unittest
from pathlib import Path

from check_gate import gate_passes
from check_repository import anchors, markdown_errors, repository_errors, title_valid
from validate_architecture import validate


class RepositoryChecksTest(unittest.TestCase):
    @staticmethod
    def core_build_files(root: Path) -> None:
        for name in ("pom.xml", "mvnw", ".mvn/wrapper/maven-wrapper.properties"):
            path = root / "services/core-api" / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("fixture")

    def test_local_links_anchors_and_reference_links(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            target = root / "target.md"
            target.write_text("# Valid heading\n# Valid heading\n")
            source = root / "source.md"
            source.write_text("[ok](target.md#valid-heading-1)\n[x][ref]\n[ref]: target.md#valid-heading\n")
            self.assertEqual(markdown_errors(root, source), [])
            source.write_text("[bad](target.md#absent)\n[bad](missing.md)\n[bad](../outside.md)")
            self.assertEqual(len(markdown_errors(root, source)), 3)

    def test_code_examples_are_not_links_or_headings(self) -> None:
        self.assertEqual(anchors("# Heading\n```md\n# Hidden\n```\n"), {"heading"})
        self.assertEqual(anchors("# The `workouts` module"), {"the-workouts-module"})

    def test_encoded_traversal_and_symlink_escape(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary) / "repo"
            root.mkdir()
            outside = Path(temporary) / "outside.md"
            outside.write_text("# External")
            (root / "linked.md").symlink_to(outside)
            source = root / "source.md"
            source.write_text("[bad](%2e%2e/outside.md)\n[bad](linked.md)")
            self.assertEqual(len(markdown_errors(root, source)), 2)

    def test_heading_suffix_collisions_get_unique_anchors(self) -> None:
        self.assertEqual(anchors("# Heading\n# Heading\n# Heading-1\n"),
                         {"heading", "heading-1", "heading-1-1"})
        self.assertEqual(anchors("# Heading\n# Heading-1\n# Heading\n"),
                         {"heading", "heading-1", "heading-2"})

    def test_checksum_mismatch_rejected_before_execution(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            war = Path(temporary) / "invalid.war"
            war.write_bytes(b"invalid executable")
            with self.assertRaisesRegex(ValueError, "checksum mismatch"):
                validate(Path(temporary) / "unused.dsl", war)

    def test_secret_error_does_not_print_credential(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            file = root / "config.txt"
            credential = "gh" + "p_" + "a" * 30
            file.write_text(credential)
            self.core_build_files(root)
            errors = repository_errors(root, [file])
            self.assertEqual(len(errors), 1)
            self.assertNotIn(credential, errors[0])

    def test_secret_in_broken_link_is_not_printed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            file = root / "source.md"
            credential = "gh" + "p_" + "a" * 30
            file.write_text(f"[credential]({credential}.md)")
            self.core_build_files(root)
            errors = repository_errors(root, [file])
            self.assertEqual(len(errors), 1)
            self.assertNotIn(credential, "\n".join(errors))

    def test_new_component_requires_real_ci(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.core_build_files(root)
            (root / "web").mkdir()
            self.assertEqual(len(repository_errors(root, [])), 1)

    def test_titles(self) -> None:
        self.assertTrue(title_valid("ci: add documentation validation"))
        self.assertFalse(title_valid("Update stuff"))
        self.assertFalse(title_valid("ci: add checks."))

    def test_enabled_core_requires_build_files_even_if_directory_is_removed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.assertEqual(len(repository_errors(root, [])), 3)
            self.core_build_files(root)
            self.assertEqual(repository_errors(root, []), [])
            (root / "services/core-api/pom.xml").unlink()
            self.assertEqual(len(repository_errors(root, [])), 1)

    def test_gate_rejects_every_nonsuccess_and_missing_job(self) -> None:
        successful = {job: {"result": "success"} for job in ("repository", "architecture", "core")}
        self.assertTrue(gate_passes(successful))
        for job in successful:
            for state in ("failure", "cancelled", "skipped"):
                with self.subTest(job=job, state=state):
                    self.assertFalse(gate_passes({**successful, job: {"result": state}}))
            self.assertFalse(gate_passes({key: value for key, value in successful.items() if key != job}))
        self.assertFalse(gate_passes({**successful, "unexpected": {"result": "success"}}))


if __name__ == "__main__":
    unittest.main()
