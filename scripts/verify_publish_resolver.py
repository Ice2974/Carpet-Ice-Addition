"""Exercise the workflow's production resolver, without publishing or network access.

Run after Gradle build: actual filenames are checked against real build/libs outputs.
No resolver implementation is copied here.
"""

import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import textwrap
import unittest
import zipfile


ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = (ROOT / ".github/workflows/publish.yml").read_text(encoding="utf-8")


def extract(text, name):
    begin, end = (f"# === {name}-{side} ===" for side in ("BEGIN", "END"))
    if text.count(begin) != 1 or text.count(end) != 1:
        raise ValueError(f"Expected exactly one marker pair: {name}")
    before, body = text.split(begin)
    if end in before:
        raise ValueError(f"Reversed marker pair: {name}")
    return textwrap.dedent(body.split(end)[0]).strip() + "\n"


NAME = "P12-PUBLISH-RESOLVER"
PRODUCTION = extract(WORKFLOW, NAME)
EXPANSION = extract(WORKFLOW, "P12-RANGE-EXPANSION")
RESOLVER = {"__name__": "production_under_test"}
exec(compile(PRODUCTION, "publish.yml:resolver", "exec"), RESOLVER)


def call(name, *args):
    return RESOLVER[name](*args)


class PublishResolverTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "tree"
        self.root.mkdir()
        self.assets = Path(self.temp.name) / "assets"
        self.assets.mkdir()
        for relative in ["settings.json", "gradle.properties"]:
            shutil.copyfile(ROOT / relative, self.root / relative)
        for version in json.loads((ROOT / "settings.json").read_text())["versions"]:
            directory = self.root / "versions" / version
            directory.mkdir(parents=True)
            shutil.copyfile(ROOT / "versions" / version / "gradle.properties", directory / "gradle.properties")

    def layout(self):
        return call("resolve_platform_layout", self.root)

    def metadata(self, layout, row):
        return {"id": layout["mod_id"], "version": layout["mod_version"],
                "depends": {"minecraft": row["minecraft_dependency"]},
                "mixins": ["carpet-ice-addition-unrelated.mixins.json"]}

    def asset(self, layout, row, metadata=None, name=None, directory=None):
        name = name or row.get("file_name", f"Carpet-Ice-Addition-v{layout['mod_version']}-mc{row['platform']}.jar")
        path = (directory or self.assets) / name
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr("fabric.mod.json", json.dumps(metadata if metadata is not None else self.metadata(layout, row)))
        return path

    def collect(self, layout=None, selected=None, event="workflow_dispatch"):
        layout = layout or self.layout()
        selected = selected or call("select_platforms", layout, "")
        return call("collect_assets", self.root, layout, selected, event, self.assets)

    def test_marker_contract(self):
        for name in [NAME, "P12-RANGE-EXPANSION"]:
            for side in ["BEGIN", "END"]:
                marker = f"# === {name}-{side} ==="
                with self.subTest(name=name, side=side, mutation="missing"):
                    with self.assertRaises(ValueError):
                        extract(WORKFLOW.replace(marker, ""), name)
                with self.subTest(name=name, side=side, mutation="duplicate"):
                    with self.assertRaises(ValueError):
                        extract(WORKFLOW + "\n" + marker, name)
            with self.assertRaises(ValueError):
                extract(f"# === {name}-END ===\n# === {name}-BEGIN ===", name)

    def test_actual_gradle_outputs_and_cli(self):
        layout = call("resolve_platform_layout", ROOT)
        selected = call("select_platforms", layout, "")
        result = call("collect_assets", ROOT, layout, selected, "release", "")
        self.assertEqual(len(result["platforms"]), len(selected))
        cli = subprocess.run([sys.executable, "-c", PRODUCTION, "collect", str(ROOT), "", "release", ""],
                             capture_output=True, text=True)
        self.assertEqual(cli.returncode, 0, cli.stderr)
        self.assertEqual(json.loads(cli.stdout), result)
        print(f"Actual Gradle runtime filenames/metadata: {len(selected)}/{len(selected)} PASS")

    def test_actual_dispatch_all_and_sources_excluded(self):
        layout = self.layout()
        for row in layout["platforms"]:
            self.asset(layout, row)
        (self.assets / "Carpet-Ice-Addition-vbroken-sources.jar").write_bytes(b"not a zip")
        self.assertEqual(len(self.collect()["platforms"]), len(layout["platforms"]))
        cli = subprocess.run([sys.executable, "-c", PRODUCTION, "collect", str(self.root), "",
                              "workflow_dispatch", str(self.assets)], capture_output=True, text=True)
        self.assertEqual(cli.returncode, 0, cli.stderr)
        self.assertEqual(json.loads(cli.stdout), self.collect())

    def test_actual_identity_never_uses_mixins(self):
        layout = self.layout()
        for row in layout["platforms"]:
            path = self.root / row["platform_dir"] / "gradle.properties"
            path.write_text("\n".join(line for line in path.read_text(encoding="utf-8").splitlines()
                                     if not line.startswith("mixin_config=")), encoding="utf-8")
        self.assertEqual(self.layout(), layout)
        for row in layout["platforms"]:
            with (self.root / row["platform_dir"] / "gradle.properties").open("a", encoding="utf-8") as stream:
                stream.write("\nmixin_config=not-a-platform-identity\n")
        self.assertEqual(self.layout(), layout)
        for mixins in [None, [], ["carpet-ice-addition-mc999.mixins.json"], "invalid runtime shape"]:
            with self.subTest(mixins=mixins):
                row = layout["platforms"][0]
                metadata = self.metadata(layout, row)
                metadata["mixins"] = mixins
                self.assertEqual(call("attribute_asset", layout, self.asset(layout, row, metadata)), row)
        # A legacy-style filename must be rejected as an unknown actual filename.
        with self.assertRaises(ValueError):
            call("attribute_asset", layout, self.asset(layout, row, name="Carpet-Ice-Addition-v3.0.0-mc1211.jar"))

    def test_selection(self):
        layout = self.layout()
        versions = [row["platform"] for row in layout["platforms"]]
        for csv in ["", " , , "]:
            self.assertEqual(call("select_platforms", layout, csv), versions)
        selected = call("select_platforms", layout, f" {versions[-1]},,{versions[0]}, {versions[-1]} ")
        self.assertEqual(selected, [versions[-1], versions[0]])
        for row in layout["platforms"]:
            if row["platform"] in selected:
                self.asset(layout, row)
        self.assertEqual([r["platform"] for r in self.collect(layout, selected)["platforms"]], selected)
        with self.assertRaises(ValueError):
            call("select_platforms", layout, "unknown")

    def test_invalid_registry(self):
        original = json.loads((self.root / "settings.json").read_text())["versions"]
        # ["mc1211"] 是纯 legacy 注册表，["mc1211", original[0]] 是混入 legacy 条目；两者都必须 fail closed。
        for versions in [[], None, "1.21.1", [1], [original[0]] * 2,
                         ["mc1211"], ["mc1211", original[0]], ["../escape"], ["unknown"]]:
            with self.subTest(versions=versions):
                (self.root / "settings.json").write_text(json.dumps({"versions": versions}))
                with self.assertRaises(ValueError):
                    self.layout()

    def test_properties_fail_closed(self):
        row = self.layout()["platforms"][0]
        path = self.root / row["platform_dir"] / "gradle.properties"
        original = path.read_text(encoding="utf-8")
        for content in [original + "\nrelease_minecraft_range=1.21.1\n",
                        "\n".join(l for l in original.splitlines() if not l.startswith("release_minecraft_range=")),
                        original.replace("release_minecraft_range=", "release_minecraft_range=invalid")]:
            with self.subTest(content=content[-100:]):
                path.write_text(content, encoding="utf-8")
                with self.assertRaises(ValueError):
                    self.layout()
        path.unlink()
        with self.assertRaises(OSError):
            self.layout()

    def test_label_collision(self):
        rows = self.layout()["platforms"]
        first = self.root / rows[0]["platform_dir"] / "gradle.properties"
        second = self.root / rows[1]["platform_dir"] / "gradle.properties"
        shutil.copyfile(first, second)
        with self.assertRaisesRegex(ValueError, "Ambiguous asset identity"):
            self.layout()

    def test_duplicate_global_property_and_cli_failure(self):
        with (self.root / "gradle.properties").open("a", encoding="utf-8") as stream:
            stream.write("\nmod_version=3.0.0\n")
        with self.assertRaises(ValueError):
            self.layout()
        cli = subprocess.run([sys.executable, "-c", PRODUCTION, "layout", str(self.root), ""],
                             capture_output=True, text=True)
        self.assertNotEqual(cli.returncode, 0)
        self.assertIn("::error::", cli.stderr)

    def test_label_examples(self):
        for value, expected in [("1.21.11", "1.21.11"), ("1.21~1.21.1", "1.21-1.21.1"),
                                ("26.1~26.1.2", "26.1.x"), ("1.21.8-1.21.7", "1.21.7-1.21.8")]:
            with self.subTest(value=value):
                self.assertEqual(call("release_label", value), expected)

    def test_bad_asset_metadata(self):
        layout = self.layout()
        row = layout["platforms"][0]
        for key, value in [("id", "wrong"), ("version", "wrong"), ("depends", {"minecraft": "wrong"}),
                           ("depends", None)]:
            with self.subTest(key=key):
                metadata = self.metadata(layout, row)
                metadata[key] = value
                with self.assertRaises(ValueError):
                    call("attribute_asset", layout, self.asset(layout, row, metadata))
        path = self.asset(layout, row)
        for payload in [b"{", b"[]"]:
            with zipfile.ZipFile(path, "w") as jar:
                jar.writestr("fabric.mod.json", payload)
            with self.assertRaises(ValueError):
                call("attribute_asset", layout, path)
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr("other.json", "{}")
        with self.assertRaises(ValueError):
            call("attribute_asset", layout, path)
        path.write_bytes(b"not a zip")
        with self.assertRaises(zipfile.BadZipFile):
            call("attribute_asset", layout, path)

    def test_missing_selected_and_wrong_actual_filename(self):
        layout = self.layout()
        row = layout["platforms"][0]
        with self.assertRaises(ValueError):
            self.collect(layout, [row["platform"]])
        self.asset(layout, row, name="Carpet-Ice-Addition-v3.0.0-mcwrong.jar")
        with self.assertRaises(ValueError):
            self.collect(layout, [row["platform"]])

    def test_release_missing_extra_and_swapped_outputs(self):
        layout = self.layout()
        for row in layout["platforms"]:
            self.asset(layout, row, directory=self.root / row["platform_dir"] / "build/libs")
        self.collect(layout, event="release")
        row = layout["platforms"][0]
        directory = self.root / row["platform_dir"] / "build/libs"
        extra = self.asset(layout, row, directory=directory, name="Carpet-Ice-Addition-v3.0.0-mcextra.jar")
        with self.assertRaises(ValueError):
            self.collect(layout, event="release")
        extra.unlink()
        (directory / row["file_name"]).unlink()
        with self.assertRaises(ValueError):
            self.collect(layout, event="release")
        self.asset(layout, layout["platforms"][1], directory=directory)
        with self.assertRaises(ValueError):
            self.collect(layout, event="release")

    def test_range_expansion_production(self):
        tags = Path(self.temp.name) / "tags.json"
        tags.write_text(json.dumps([{"version": v, "version_type": "release"}
                                   for v in ["1.21", "1.21.1", "26.1", "26.1.1", "26.1.2", "26.2"]]))
        env = dict(os.environ, MODRINTH_GAME_VERSION_TAGS_FILE=str(tags))
        for directory, expected in [("versions/1.21.1", ["1.21", "1.21.1"]),
                                    ("versions/26.1.2", ["26.1", "26.1.1", "26.1.2"])]:
            result = subprocess.run([sys.executable, "-c", EXPANSION, str(self.root / directory)],
                                    capture_output=True, text=True, env=env)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(json.loads(result.stdout), expected)
        path = self.root / "versions/1.21.1" / "gradle.properties"
        path.write_text(path.read_text(encoding="utf-8").replace("release_minecraft_range=1.21~1.21.1",
                                                               "release_minecraft_range=26.2"), encoding="utf-8")
        result = subprocess.run([sys.executable, "-c", EXPANSION, str(path.parent)],
                                capture_output=True, text=True, env=env)
        self.assertNotEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
