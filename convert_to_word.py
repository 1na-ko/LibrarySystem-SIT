#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Markdown to Word Document Converter for Library System Testing Report.

Reads 软件测试总结报告.md and converts it to a properly formatted .docx file
using python-docx, with embedded images, formatted tables, and Chinese fonts.
"""

import re
import os
from docx import Document
from docx.shared import Pt, Inches, Cm, RGBColor, Emu
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml.ns import qn, nsdecls
from docx.oxml import parse_xml

# ============================================================================
# Configuration
# ============================================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MD_FILE = os.path.join(BASE_DIR, "软件测试总结报告.md")
IMAGES_DIR = os.path.join(BASE_DIR, "report_images")
OUTPUT_FILE = os.path.join(BASE_DIR, "软件测试总结报告.docx")

# Font settings
BODY_FONT_NAME = "SimSun"
BODY_FONT_NAME_EAST_ASIAN = "宋体"
HEADING_FONT_NAME = "SimHei"
HEADING_FONT_NAME_EAST_ASIAN = "黑体"
CODE_FONT_NAME = "Consolas"
CODE_FONT_SIZE = Pt(9)

# Page settings
PAGE_MARGIN_CM = Cm(2.5)
LINE_SPACING = 1.5

# Heading font sizes (in Pt)
HEADING_SIZES = {
    1: Pt(22),
    2: Pt(18),
    3: Pt(15),
    4: Pt(13),
}


# ============================================================================
# Utility functions
# ============================================================================

def set_cell_border(cell, **kwargs):
    """Set cell border. Usage: set_cell_border(cell, top={"sz": 12, "val": "single", "color": "000000"})"""
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    tcBorders = parse_xml(f'<w:tcBorders {nsdecls("w")}></w:tcBorders>')
    for edge, attrs in kwargs.items():
        element = parse_xml(
            f'<w:{edge} {nsdecls("w")} w:val="{attrs.get("val", "single")}" '
            f'w:sz="{attrs.get("sz", 4)}" w:space="0" '
            f'w:color="{attrs.get("color", "000000")}"/>'
        )
        tcBorders.append(element)
    tcPr.append(tcBorders)


def set_table_borders(table):
    """Set all borders for a table with thin black lines."""
    border_attrs = {"val": "single", "sz": "4", "color": "000000"}
    for row in table.rows:
        for cell in row.cells:
            set_cell_border(
                cell,
                top=border_attrs,
                bottom=border_attrs,
                start=border_attrs,
                end=border_attrs,
            )


def set_run_font(run, font_name=None, font_name_east=None, size=None, bold=None, color=None):
    """Set font properties for a run, including East-Asian font."""
    if font_name:
        run.font.name = font_name
    if font_name_east:
        rPr = run._element.get_or_add_rPr()
        rFonts = rPr.find(qn('w:rFonts'))
        if rFonts is None:
            rFonts = parse_xml(f'<w:rFonts {nsdecls("w")}/>')
            rPr.insert(0, rFonts)
        rFonts.set(qn('w:eastAsia'), font_name_east)
    if size:
        run.font.size = size
    if bold is not None:
        run.bold = bold
    if color:
        run.font.color.rgb = color


def add_paragraph_with_font(doc, text, font_name=BODY_FONT_NAME, font_east=BODY_FONT_NAME_EAST_ASIAN,
                             size=Pt(11), bold=False, alignment=None, spacing_after=Pt(6),
                             first_line_indent=None):
    """Add a paragraph with specified Chinese font settings."""
    para = doc.add_paragraph()
    if alignment is not None:
        para.alignment = alignment
    para.paragraph_format.space_after = spacing_after
    para.paragraph_format.line_spacing = LINE_SPACING
    if first_line_indent:
        para.paragraph_format.first_line_indent = first_line_indent
    run = para.add_run(text)
    set_run_font(run, font_name=font_name, font_name_east=font_east, size=size, bold=bold)
    return para


def add_heading_styled(doc, text, level):
    """Add a heading with SimHei font and proper size."""
    heading = doc.add_heading(level=level)
    run = heading.add_run(text)
    size = HEADING_SIZES.get(level, Pt(14))
    set_run_font(run, font_name=HEADING_FONT_NAME, font_name_east=HEADING_FONT_NAME_EAST_ASIAN,
                  size=size, bold=True)
    heading.paragraph_format.space_before = Pt(12)
    heading.paragraph_format.space_after = Pt(6)
    heading.paragraph_format.line_spacing = LINE_SPACING
    return heading


def add_code_block(doc, code_lines):
    """Add a code block as a paragraph with monospace font."""
    code_text = "\n".join(code_lines)
    para = doc.add_paragraph()
    para.paragraph_format.space_after = Pt(6)
    para.paragraph_format.line_spacing = 1.0
    para.paragraph_format.left_indent = Cm(1)
    # Light gray background via shading
    pPr = para._element.get_or_add_pPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="F5F5F5" w:val="clear"/>')
    pPr.append(shd)
    run = para.add_run(code_text)
    set_run_font(run, font_name=CODE_FONT_NAME, font_name_east=BODY_FONT_NAME_EAST_ASIAN,
                  size=CODE_FONT_SIZE)
    return para


def add_image_paragraph(doc, image_path, alt_text=""):
    """Add an image as a centered paragraph."""
    full_path = os.path.join(IMAGES_DIR, os.path.basename(image_path)) if not os.path.isabs(image_path) else image_path
    if not os.path.exists(full_path):
        # Try relative path from report_images
        full_path = os.path.join(IMAGES_DIR, os.path.basename(image_path))
    if os.path.exists(full_path):
        para = doc.add_paragraph()
        para.alignment = WD_ALIGN_PARAGRAPH.CENTER
        para.paragraph_format.space_before = Pt(6)
        para.paragraph_format.space_after = Pt(6)
        run = para.add_run()
        try:
            run.add_picture(full_path, width=Inches(5.5))
        except Exception as e:
            print(f"  Warning: Could not embed image {full_path}: {e}")
            run.add_text(f"[Image: {alt_text or os.path.basename(image_path)}]")
        if alt_text:
            # Add caption
            cap_para = doc.add_paragraph()
            cap_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
            cap_para.paragraph_format.space_after = Pt(10)
            cap_run = cap_para.add_run(alt_text)
            set_run_font(cap_run, font_name=BODY_FONT_NAME, font_name_east=BODY_FONT_NAME_EAST_ASIAN,
                          size=Pt(9), bold=False, color=RGBColor(0x66, 0x66, 0x66))
        return para
    else:
        print(f"  Warning: Image not found: {full_path}")
        para = doc.add_paragraph()
        para.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = para.add_run(f"[Image not found: {alt_text or image_path}]")
        set_run_font(run, font_name=BODY_FONT_NAME, font_name_east=BODY_FONT_NAME_EAST_ASIAN,
                      size=Pt(10), color=RGBColor(0xCC, 0x00, 0x00))
        return para


# ============================================================================
# Markdown Parser
# ============================================================================


def parse_table_row(line):
    """Parse a markdown table row, returning list of cell contents."""
    line = line.strip()
    if line.startswith("|"):
        line = line[1:]
    if line.endswith("|"):
        line = line[:-1]
    return [cell.strip() for cell in line.split("|")]


def is_table_separator(line):
    """Check if a line is a markdown table separator like |:---|:---:|"""
    stripped = line.strip()
    if not stripped.startswith("|") or not stripped.endswith("|"):
        return False
    inner = stripped[1:-1]
    parts = inner.split("|")
    return all(re.match(r'^:?-{2,}:?$', p.strip()) for p in parts)


def process_inline_formatting(run, text, default_font=BODY_FONT_NAME, default_east=BODY_FONT_NAME_EAST_ASIAN,
                               default_size=Pt(11)):
    """Process bold (**text**) in inline text and add to run."""
    # Split by bold markers
    parts = re.split(r'(\*\*.*?\*\*)', text)
    for part in parts:
        if part.startswith("**") and part.endswith("**"):
            # Bold text
            inner = part[2:-2]
            sub_run = run
            set_run_font(sub_run, font_name=default_font, font_name_east=default_east,
                          size=default_size, bold=True)
            # We can't easily split a run mid-text in python-docx
            # So we'll handle bold at paragraph level instead
            pass
    return text


# ============================================================================
# Main Conversion
# ============================================================================


def convert_md_to_docx():
    """Main conversion function."""
    print(f"Reading: {MD_FILE}")

    with open(MD_FILE, "r", encoding="utf-8") as f:
        lines = f.readlines()

    doc = Document()

    # ---- Page setup ----
    section = doc.sections[0]
    section.top_margin = PAGE_MARGIN_CM
    section.bottom_margin = PAGE_MARGIN_CM
    section.left_margin = PAGE_MARGIN_CM
    section.right_margin = PAGE_MARGIN_CM

    # ---- Configure default style ----
    style = doc.styles['Normal']
    style.font.name = BODY_FONT_NAME
    style.font.size = Pt(11)
    style.element.rPr.rFonts.set(qn('w:eastAsia'), BODY_FONT_NAME_EAST_ASIAN)
    style.paragraph_format.line_spacing = LINE_SPACING

    # Process lines
    i = 0
    in_code_block = False
    code_lines = []
    in_table = False
    table_rows = []
    in_html_div = False

    while i < len(lines):
        line = lines[i].rstrip("\n")

        # ---- Skip HTML div tags ----
        if line.strip() == '<div align="center">':
            in_html_div = True
            i += 1
            continue
        if line.strip() == '</div>':
            in_html_div = False
            i += 1
            continue
        if in_html_div:
            # Process content inside div as normal markdown
            pass

        # ---- Code blocks ----
        if line.strip().startswith("```"):
            if in_code_block:
                # End code block
                if code_lines:
                    add_code_block(doc, code_lines)
                code_lines = []
                in_code_block = False
            else:
                in_code_block = True
            i += 1
            continue

        if in_code_block:
            code_lines.append(line)
            i += 1
            continue

        # ---- Horizontal rules ----
        if re.match(r'^-{3,}$', line.strip()):
            para = doc.add_paragraph()
            para.paragraph_format.space_before = Pt(6)
            para.paragraph_format.space_after = Pt(6)
            # Add a bottom border as horizontal rule
            pPr = para._element.get_or_add_pPr()
            pBdr = parse_xml(
                f'<w:pBdr {nsdecls("w")}>'
                f'<w:bottom w:val="single" w:sz="6" w:space="1" w:color="999999"/>'
                f'</w:pBdr>'
            )
            pPr.append(pBdr)
            i += 1
            continue

        # ---- Table detection ----
        if line.strip().startswith("|") and line.strip().endswith("|"):
            # Check if next line is separator
            if i + 1 < len(lines) and is_table_separator(lines[i + 1].rstrip("\n")):
                if not in_table:
                    in_table = True
                    table_rows = []
                table_rows.append(parse_table_row(line))
                i += 1
                continue
            elif in_table:
                # Continue table rows
                table_rows.append(parse_table_row(line))
                i += 1
                continue
        elif is_table_separator(line):
            # This is the separator line, skip it
            i += 1
            continue
        elif in_table and line.strip() == "":
            # End of table - render it
            if table_rows:
                render_table(doc, table_rows)
            in_table = False
            table_rows = []
            i += 1
            continue
        elif in_table and not line.strip().startswith("|"):
            # End of table
            if table_rows:
                render_table(doc, table_rows)
            in_table = False
            table_rows = []
            # Don't increment i - re-process this line
            continue

        # ---- Empty lines ----
        if line.strip() == "":
            i += 1
            continue

        # ---- Headings ----
        heading_match = re.match(r'^(#{1,4})\s+(.+)$', line)
        if heading_match:
            level = len(heading_match.group(1))
            text = heading_match.group(2).strip()
            add_heading_styled(doc, text, level)
            i += 1
            continue

        # ---- Images ----
        image_match = re.match(r'^!\[(.+?)\]\((.+?)\)$', line.strip())
        if image_match:
            alt_text = image_match.group(1)
            image_path = image_match.group(2)
            add_image_paragraph(doc, image_path, alt_text)
            i += 1
            continue

        # ---- Regular paragraph ----
        # Process the paragraph text - handle **bold**, etc.
        para_text = line.strip()
        add_paragraph_with_bold(doc, para_text)
        i += 1

    # Handle any leftover table at EOF
    if in_table and table_rows:
        render_table(doc, table_rows)

    # ---- Save ----
    print(f"Saving to: {OUTPUT_FILE}")
    doc.save(OUTPUT_FILE)
    print("Conversion complete!")


def render_table(doc, rows):
    """Render a markdown table as a Word table with borders."""
    if not rows:
        return

    num_cols = max(len(row) for row in rows)
    table = doc.add_table(rows=len(rows), cols=num_cols)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = True

    # Set table style for borders
    set_table_borders(table)

    for r_idx, row_data in enumerate(rows):
        row = table.rows[r_idx]
        for c_idx, cell_text in enumerate(row_data):
            if c_idx >= num_cols:
                break
            cell = row.cells[c_idx]
            # Clear default paragraph
            cell.paragraphs[0].clear()
            para = cell.paragraphs[0]
            para.paragraph_format.space_before = Pt(2)
            para.paragraph_format.space_after = Pt(2)
            para.paragraph_format.line_spacing = 1.15

            # Determine if it's a header row (first row)
            is_header = (r_idx == 0)
            run = para.add_run(cell_text)
            set_run_font(run, font_name=BODY_FONT_NAME, font_name_east=BODY_FONT_NAME_EAST_ASIAN,
                          size=Pt(9), bold=is_header)

            # Center-align first column and header row
            if c_idx == 0 or is_header:
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER
            else:
                para.alignment = WD_ALIGN_PARAGRAPH.LEFT

            # Set cell shading for header
            if is_header:
                tcPr = cell._tc.get_or_add_tcPr()
                shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="D9E2F3" w:val="clear"/>')
                tcPr.append(shd)

    # Add a small gap after the table
    doc.add_paragraph()


def add_paragraph_with_bold(doc, text):
    """Add a paragraph, handling **bold** markers."""
    para = doc.add_paragraph()
    para.paragraph_format.line_spacing = LINE_SPACING
    para.paragraph_format.space_after = Pt(6)
    para.paragraph_format.first_line_indent = Cm(0.74)  # Two-character indent for body text

    # Split by bold markers and handle
    parts = re.split(r'(\*\*.*?\*\*)', text)
    for part in parts:
        if part.startswith("**") and part.endswith("**"):
            inner = part[2:-2]
            run = para.add_run(inner)
            set_run_font(run, font_name=BODY_FONT_NAME, font_name_east=BODY_FONT_NAME_EAST_ASIAN,
                          size=Pt(11), bold=True)
        else:
            if part:  # Skip empty strings
                run = para.add_run(part)
                set_run_font(run, font_name=BODY_FONT_NAME, font_name_east=BODY_FONT_NAME_EAST_ASIAN,
                              size=Pt(11))


# ============================================================================
# Entry point
# ============================================================================

if __name__ == "__main__":
    convert_md_to_docx()
