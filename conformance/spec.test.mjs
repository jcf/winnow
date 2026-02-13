import { readFileSync } from "fs";
import { describe, expect, test } from "vitest";
import { twMerge } from "tailwind-merge";

const SPEC_PATH = "../spec/winnow.txt";

function parseSpec(content) {
  const lines = content.split("\n");
  const cases = [];
  let currentSection = "Uncategorized";
  let currentInput = null;
  let inputLine = null;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const lineNum = i + 1;

    // Section headers (lines starting with # followed by =)
    if (line.startsWith("# ==")) {
      continue;
    }

    // Section name (comment after section delimiter)
    if (line.startsWith("# ") && lines[i - 1]?.startsWith("# ==")) {
      currentSection = line.slice(2).trim();
      continue;
    }

    // Skip other comments and blank lines
    if (line.startsWith("#") || line.trim() === "") {
      continue;
    }

    // Expected output line
    if (line.startsWith("=>")) {
      if (currentInput === null) {
        throw new Error(`Line ${lineNum}: Found => without input`);
      }
      const expected = line.slice(2).trim();
      cases.push({
        section: currentSection,
        input: currentInput,
        expected,
        line: inputLine,
      });
      currentInput = null;
      inputLine = null;
      continue;
    }

    // Input line - may contain multiple strings separated by " | "
    if (currentInput !== null) {
      throw new Error(
        `Line ${lineNum}: Found input without => for previous input at line ${inputLine}`
      );
    }
    currentInput = line.trim().split(" | ");
    inputLine = lineNum;
  }

  if (currentInput !== null) {
    throw new Error(`End of file: Found input at line ${inputLine} without =>`);
  }

  return cases;
}

function groupBySection(cases) {
  const sections = new Map();
  for (const c of cases) {
    if (!sections.has(c.section)) {
      sections.set(c.section, []);
    }
    sections.get(c.section).push(c);
  }
  return sections;
}

const content = readFileSync(SPEC_PATH, "utf-8");
const cases = parseSpec(content);
const sections = groupBySection(cases);

for (const [section, sectionCases] of sections) {
  describe(section, () => {
    for (const { input, expected, line } of sectionCases) {
      const inputStr = input.join(" | ");
      test(`line ${line}: ${inputStr} => ${expected}`, () => {
        expect(twMerge(...input)).toBe(expected);
      });
    }
  });
}
