"""Render the evidence-labeled assignment report draft from its Markdown source.

Run with the bundled Python environment: python docs/report/build_report.py
Do not rename the output to FINAL until public links and missing captures are verified.
"""

from __future__ import annotations

import html
import re
from pathlib import Path

from PIL import Image as PILImage
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Flowable,
    Image,
    KeepTogether,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
)


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "docs/report/assignment-report-draft.md"
OUTPUT = ROOT / "output/pdf/CertChain-assignment-report-DRAFT.pdf"
SCRATCH = ROOT / "tmp/pdfs/phase14"
INK = colors.HexColor("#182c2d")
TEAL = colors.HexColor("#0b625d")
PALE = colors.HexColor("#eaf4f1")
MID = colors.HexColor("#607573")
LINE = colors.HexColor("#c9d9d5")
PAPER = colors.HexColor("#fbfaf7")


def fonts() -> None:
    folder = ROOT / "backend/src/main/resources/fonts"
    pdfmetrics.registerFont(TTFont("Noto", str(folder / "NotoSans-Regular.ttf")))
    pdfmetrics.registerFont(TTFont("Noto-Bold", str(folder / "NotoSans-Bold.ttf")))
    pdfmetrics.registerFontFamily("Noto", normal="Noto", bold="Noto-Bold")


def styles() -> dict[str, ParagraphStyle]:
    base = dict(textColor=INK, allowWidows=0, allowOrphans=0)
    return {
        "title": ParagraphStyle("Title", **base, fontName="Noto-Bold", fontSize=30, leading=36, spaceAfter=10),
        "subtitle": ParagraphStyle("Subtitle", **{**base, "textColor": MID}, fontName="Noto",
                                    fontSize=12, leading=18, spaceAfter=18),
        "h2": ParagraphStyle("Section", **{**base, "textColor": TEAL}, fontName="Noto-Bold", fontSize=14.5, leading=20,
                             spaceBefore=15, spaceAfter=8, keepWithNext=True),
        "body": ParagraphStyle("Body", **base, fontName="Noto", fontSize=9.2, leading=14.4, spaceAfter=8),
        "bullet": ParagraphStyle("Bullet", **base, fontName="Noto", fontSize=9.2, leading=14.4, leftIndent=13,
                                 firstLineIndent=-9, spaceAfter=5),
        "caption": ParagraphStyle("Caption", **{**base, "textColor": MID}, fontName="Noto", fontSize=8.1, leading=11.5,
                                  alignment=TA_CENTER, spaceBefore=5, spaceAfter=13),
        "diagram": ParagraphStyle("Diagram caption", **{**base, "textColor": MID}, fontName="Noto", fontSize=8.2, leading=12,
                                  alignment=TA_LEFT, spaceAfter=7),
        "callout": ParagraphStyle("Callout", **{**base, "textColor": TEAL}, fontName="Noto-Bold", fontSize=10,
                                 leading=15, backColor=PALE, borderPadding=11,
                                 spaceBefore=8, spaceAfter=16),
    }


LINK = re.compile(r"\[([^\]]+)\]\((https?://[^)]+)\)")


def rich(text: str) -> str:
    links: list[str] = []

    def replace_link(match: re.Match[str]) -> str:
        label, url = match.groups()
        token = f"__LINK_{len(links)}__"
        links.append(f'<link href="{html.escape(url, quote=True)}" color="#0b625d">'
                     f"<u>{html.escape(label)}</u></link>")
        return token

    rendered = html.escape(LINK.sub(replace_link, text), quote=False)
    rendered = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", rendered)
    rendered = re.sub(r"`([^`]+)`", r'<font name="Courier" color="#284744">\1</font>', rendered)
    for number, link in enumerate(links):
        rendered = rendered.replace(f"__LINK_{number}__", link)
    return rendered


def arrow(c, x1: float, y1: float, x2: float, y2: float) -> None:
    import math

    c.setStrokeColor(TEAL)
    c.setFillColor(TEAL)
    c.setLineWidth(1.2)
    c.line(x1, y1, x2, y2)
    angle = math.atan2(y2 - y1, x2 - x1)
    size = 5
    path = c.beginPath()
    path.moveTo(x2, y2)
    path.lineTo(x2 - size * math.cos(angle - .45), y2 - size * math.sin(angle - .45))
    path.lineTo(x2 - size * math.cos(angle + .45), y2 - size * math.sin(angle + .45))
    path.close()
    c.drawPath(path, fill=1, stroke=0)


def box(c, x: float, y: float, width: float, height: float, title: str,
        lines: tuple[str, ...] = ()) -> None:
    c.setFillColor(PAPER)
    c.setStrokeColor(LINE)
    c.roundRect(x, y, width, height, 7, stroke=1, fill=1)
    c.setFillColor(TEAL)
    c.setFont("Noto-Bold", 8.2)
    c.drawString(x + 9, y + height - 17, title)
    c.setFillColor(MID)
    c.setFont("Noto", 7.3)
    for index, line in enumerate(lines):
        c.drawString(x + 9, y + height - 31 - index * 11, line)


class Architecture(Flowable):
    def __init__(self, width: float) -> None:
        super().__init__()
        self.width, self.height = width, 220

    def draw(self) -> None:
        c = self.canv
        box(c, 4, 137, 103, 49, "Admin", ("authenticated portal",))
        box(c, 4, 55, 103, 49, "Public verifier", ("ID or QR",))
        box(c, 131, 96, 107, 55, "Next.js", ("portal + public UI",))
        box(c, 265, 96, 112, 55, "Spring Boot", ("auth + workflows",))
        box(c, 400, 170, 109, 42, "PostgreSQL", ("private records",))
        box(c, 400, 116, 109, 42, "Private PDFs", ("artifact storage",))
        box(c, 400, 62, 109, 42, "SMTP", ("recipient email",))
        box(c, 400, 8, 109, 42, "Ethereum", ("RPC + registry",))
        arrow(c, 107, 160, 131, 135)
        arrow(c, 107, 79, 131, 112)
        arrow(c, 238, 123, 265, 123)
        for target in (190, 137, 83, 29):
            arrow(c, 377, 123, 400, target)


class ERDiagram(Flowable):
    def __init__(self, width: float) -> None:
        super().__init__()
        self.width, self.height = width, 312

    def draw(self) -> None:
        c = self.canv
        box(c, 6, 180, 148, 74, "organization", ("id UUID PK", "name, email"))
        box(c, 181, 231, 149, 74, "app_user", ("id UUID PK", "organization_id FK", "email unique (casefold)"))
        box(c, 181, 120, 149, 95, "certificate", ("id UUID PK", "organization_id FK", "certificate_id unique", "lifecycle + proof hash"))
        box(c, 358, 201, 151, 84, "blockchain_transaction", ("certificate_id FK", "tx hash unique", "chain + receipt journal"))
        box(c, 358, 99, 151, 84, "email_delivery", ("certificate_id FK", "status + attempts"))
        box(c, 6, 13, 167, 74, "certificate_number_sequence", ("sequence_year PK", "global next_value"))
        arrow(c, 154, 219, 181, 267)
        arrow(c, 154, 207, 181, 168)
        arrow(c, 330, 179, 358, 238)
        arrow(c, 330, 153, 358, 141)
        c.setFillColor(MID)
        c.setFont("Noto", 7.2)
        c.drawString(9, 97, "Yearly counter is independent of tenant.")


STEPS = {
    "Issuance": [
        "Admin confirms a tenant-owned draft",
        "Backend freezes fields, computes SHA-256, writes CREATED journal",
        "Issuer sends issueCertificate; transaction hash becomes SUBMITTED",
        "Backend validates receipt, confirmations, event, and on-chain record",
        "Journal becomes CONFIRMED; certificate becomes ISSUED",
        "PDF/QR and recipient email run afterward; failures can retry",
    ],
    "Verification": [
        "Visitor enters strict public ID or opens its QR URL",
        "Backend loads issued record and authoritative journal",
        "Canonical data is rebuilt and SHA-256 is recomputed",
        "Backend reads deterministic key from the expected registry",
        "Stored and on-chain proof, network, issuer, and revoke flag are checked",
        "Return VERIFIED + derived status, or MISMATCH / UNAVAILABLE",
    ],
    "Revocation": [
        "Admin enters a private reason and explicitly confirms",
        "Backend locks issued record and writes CREATED revoke journal",
        "Issuer sends revokeCertificate; hash becomes SUBMITTED",
        "Backend validates successful receipt and CertificateRevoked event",
        "Journal becomes CONFIRMED; only now is revokedAt saved",
        "Public verification derives REVOKED before EXPIRED",
    ],
}


class StepsDiagram(Flowable):
    def __init__(self, width: float, kind: str) -> None:
        super().__init__()
        self.width, self.height, self.kind = width, 245, kind

    def draw(self) -> None:
        c = self.canv
        for index, label in enumerate(STEPS[self.kind]):
            y = 207 - index * 39
            c.setFillColor(PALE if index % 2 == 0 else PAPER)
            c.setStrokeColor(LINE)
            c.roundRect(5, y, self.width - 10, 30, 5, stroke=1, fill=1)
            c.setFillColor(TEAL)
            c.setFont("Noto-Bold", 9)
            c.drawString(17, y + 9, f"{index + 1:02d}")
            c.setFillColor(INK)
            c.setFont("Noto", 8.3)
            c.drawString(47, y + 9, label)
            if index < 5:
                arrow(c, self.width / 2, y - 1, self.width / 2, y - 8)


def screenshot(path: Path, title: str, st: dict[str, ParagraphStyle]) -> KeepTogether:
    SCRATCH.mkdir(parents=True, exist_ok=True)
    image = PILImage.open(path).convert("RGB")
    name = path.name
    if name == "phase12-not-found.png":
        image = image.crop((470, 42, 970, 310))
    elif name.startswith("phase12-"):
        image = image.crop((330, 38, 1120, 800))
    elif name == "phase14-login-local.png":
        image = image.crop((415, 175, 865, 725))
    processed = SCRATCH / name
    image.save(processed, quality=90)
    max_width, max_height = (350, 365) if name == "phase14-login-local.png" else (455, 570)
    ratio = min(max_width / image.width, max_height / image.height)
    display = Image(str(processed), width=image.width * ratio, height=image.height * ratio)
    display.hAlign = "CENTER"
    caption = Paragraph(rich(title), st["caption"])
    return KeepTogether([display, caption])


def page_frame(c, doc) -> None:
    width, height = A4
    c.saveState()
    c.setStrokeColor(LINE)
    c.setLineWidth(.7)
    c.line(44, height - 40, width - 44, height - 40)
    c.setFont("Noto-Bold", 8)
    c.setFillColor(TEAL)
    c.drawString(45, height - 31, "CERTCHAIN / ASSIGNMENT REPORT")
    c.setFont("Noto", 8)
    c.setFillColor(MID)
    c.drawRightString(width - 45, height - 31, "EVIDENCE DRAFT")
    c.line(44, 42, width - 44, 42)
    c.drawString(45, 29, "Local evidence only - public deployment and video pending")
    c.drawRightString(width - 45, 29, f"Page {doc.page}")
    c.restoreState()


def build() -> None:
    fonts()
    st = styles()
    text = SOURCE.read_text(encoding="utf-8")
    headings = re.findall(r"^## (.+)$", text, flags=re.MULTILINE)
    assert len(headings) == 11 and [h.split(". ", 1)[0] for h in headings] == [str(i) for i in range(1, 12)]

    story: list[Flowable] = [
        Spacer(1, 23),
        Paragraph("CertChain", st["title"]),
        Paragraph("Digital certificate issuance and public proof verification", st["subtitle"]),
        Paragraph("Assignment report | 29 September 2026", st["body"]),
        Paragraph("EVIDENCE DRAFT - Public deployment, Sepolia proof, complete portal captures, and video URL are pending.", st["callout"]),
    ]
    lines = text.splitlines()
    index = 0
    paragraph: list[str] = []
    flow_index = 0
    current_section = 0

    def flush() -> None:
        if paragraph:
            story.append(Paragraph(rich(" ".join(paragraph)), st["body"]))
            paragraph.clear()

    while index < len(lines):
        line = lines[index].strip()
        if line.startswith("## "):
            flush()
            current_section += 1
            if current_section in (4, 7, 9):
                story.append(Spacer(1, 9))
            story.append(Paragraph(rich(line[3:]), st["h2"]))
        elif line.startswith("```mermaid"):
            flush()
            block: list[str] = []
            index += 1
            while index < len(lines) and not lines[index].strip().startswith("```"):
                block.append(lines[index])
                index += 1
            kind = "Architecture" if "flowchart" in block[0] else "ER" if "erDiagram" in block[0] else (
                "Issuance", "Verification", "Revocation")[flow_index]
            if "sequenceDiagram" in block[0]:
                flow_index += 1
            caption = Paragraph(f"Figure - {kind} flow", st["diagram"])
            diagram = Architecture(510) if kind == "Architecture" else ERDiagram(510) if kind == "ER" else StepsDiagram(510, kind)
            story.append(KeepTogether([caption, diagram]))
            story.append(Spacer(1, 12))
        elif line.startswith("!["):
            flush()
            match = re.match(r"!\[([^\]]+)\]\(([^)]+)\)", line)
            if not match:
                raise ValueError(f"Unrecognized image line: {line}")
            title, relative = match.groups()
            path = (SOURCE.parent / relative).resolve()
            if not path.is_file():
                raise FileNotFoundError(path)
            story.append(screenshot(path, title, st))
        elif line.startswith("- "):
            flush()
            story.append(Paragraph("&#8226; " + rich(line[2:]), st["bullet"]))
        elif not line:
            flush()
        elif current_section:
            paragraph.append(line)
        index += 1
    flush()

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc = SimpleDocTemplate(str(OUTPUT), pagesize=A4, rightMargin=40, leftMargin=40,
                            topMargin=55, bottomMargin=55, title="CertChain Assignment Report - Evidence Draft",
                            author="CertChain contributors")
    doc.build(story, onFirstPage=page_frame, onLaterPages=page_frame)
    print(f"Created {OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    build()
