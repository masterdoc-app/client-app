#!/usr/bin/env python3
"""Generate polished RuStore screenshots and icon from smoke captures."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent
UPLOAD = OUT / "upload"

W, H = 1080, 1920
BG = (18, 24, 32)  # #121820
ACCENT = (59, 158, 255)  # #3B9EFF
ACCENT_DIM = (43, 120, 200)
TEXT = (230, 236, 245)
TEXT_DIM = (140, 152, 168)
FRAME_BG = (22, 30, 42)

STATUS_H = 52
BRAND_H = 72
CAPTION_H = 130
FRAME_PAD_X = 56
FRAME_TOP = STATUS_H + BRAND_H + 24
FRAME_BOTTOM = CAPTION_H + 40
RADIUS = 36


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Supplemental/Helvetica.ttc",
        "/Library/Fonts/Arial Unicode.ttf",
    ]
    for path in candidates:
        p = Path(path)
        if p.exists():
            return ImageFont.truetype(str(p), size=size)
    return ImageFont.load_default()


def cover_rect(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], color=(255, 255, 255)) -> None:
    x0, y0, x1, y1 = box
    draw.rectangle(box, fill=color)


def crop_main_content(
    src: Image.Image,
    sidebar_px: int = 130,
    box: tuple[int, int, int, int] | None = None,
) -> Image.Image:
    w, h = src.size
    if box is None:
        return src.crop((sidebar_px, 0, w, h))
    return src.crop(box)


def fit_contain(img: Image.Image, tw: int, th: int, bg=(248, 250, 252)) -> Image.Image:
    """Fit entire UI into frame with light letterboxing (desktop → portrait)."""
    iw, ih = img.size
    scale = min(tw / iw, th / ih)
    nw, nh = max(1, int(iw * scale)), max(1, int(ih * scale))
    resized = img.resize((nw, nh), Image.Resampling.LANCZOS)
    canvas = Image.new("RGB", (tw, th), bg)
    canvas.paste(resized, ((tw - nw) // 2, (th - nh) // 2))
    return canvas


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius=radius, fill=255)
    return mask


def draw_status_bar(base: Image.Image, draw: ImageDraw.ImageDraw, x0: int, y0: int, fw: int) -> None:
    font = load_font(26)
    draw.text((x0 + 28, y0 + 14), "9:41", fill=TEXT, font=font)
    bx = x0 + fw - 88
    by = y0 + 18
    draw.rounded_rectangle((bx, by, bx + 44, by + 20), radius=4, outline=TEXT_DIM, width=2)
    draw.rectangle((bx + 44, by + 6, bx + 48, by + 14), fill=TEXT_DIM)


def draw_brand(draw: ImageDraw.ImageDraw, y: int) -> None:
    font = load_font(34, bold=True)
    tw = draw.textlength("Fixaverse", font=font)
    draw.text(((W - tw) / 2, y), "Fixaverse", fill=ACCENT, font=font)


def draw_caption(draw: ImageDraw.ImageDraw, y: int, title: str, subtitle: str) -> None:
    title_font = load_font(52, bold=True)
    sub_font = load_font(28)
    tw = draw.textlength(title, font=title_font)
    draw.text(((W - tw) / 2, y), title, fill=TEXT, font=title_font)
    sw = draw.textlength(subtitle, font=sub_font)
    draw.text(((W - sw) / 2, y + 62), subtitle, fill=TEXT_DIM, font=sub_font)


def add_vignette(frame: Image.Image) -> Image.Image:
    overlay = Image.new("RGBA", frame.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    fw, fh = frame.size
    for i in range(24):
        alpha = int(18 * (i / 24))
        inset = i * 3
        draw.rounded_rectangle(
            (inset, inset, fw - inset, fh - inset),
            radius=max(RADIUS - inset, 0),
            outline=(0, 0, 0, alpha),
            width=2,
        )
    return Image.alpha_composite(frame.convert("RGBA"), overlay)


def compose_screenshot(
    src_path: Path,
    out_path: Path,
    caption: str,
    subtitle: str,
    sidebar_px: int = 130,
    crop_box: tuple[int, int, int, int] | None = None,
    cover_boxes: list[tuple[int, int, int, int]] | None = None,
    focus_y: float = 0.5,
) -> None:
    src = Image.open(src_path).convert("RGB")
    content = crop_main_content(src, sidebar_px, crop_box)

    if cover_boxes:
        draw_src = ImageDraw.Draw(content)
        for box in cover_boxes:
            cover_rect(draw_src, box, color=(241, 245, 249))

    frame_w = W - FRAME_PAD_X * 2
    frame_h = H - FRAME_TOP - FRAME_BOTTOM

    ui = fit_contain(content, frame_w, frame_h)

    canvas = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(canvas)

    # Subtle top gradient accent
    grad = Image.new("RGBA", (W, 420), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grad)
    for y in range(420):
        a = int(36 * (1 - y / 420))
        gd.line([(0, y), (W, y)], fill=(ACCENT[0], ACCENT[1], ACCENT[2], a))
    canvas.paste(Image.alpha_composite(Image.new("RGBA", (W, 420), (*BG, 255)), grad).convert("RGB"), (0, 0))

    draw = ImageDraw.Draw(canvas)
    draw_brand(draw, STATUS_H + 8)

    fx = FRAME_PAD_X
    fy = FRAME_TOP
    shadow = Image.new("RGBA", (frame_w + 24, frame_h + 24), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle((12, 12, frame_w + 12, frame_h + 12), radius=RADIUS + 4, fill=(0, 0, 0, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(10))
    canvas.paste(shadow, (fx - 12, fy - 6), shadow)

    frame = Image.new("RGBA", (frame_w, frame_h), (*FRAME_BG, 255))
    frame.paste(ui, (0, 0))
    frame = add_vignette(frame)
    mask = rounded_mask((frame_w, frame_h), RADIUS)
    canvas.paste(frame.convert("RGB"), (fx, fy), mask)

    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((fx, fy, fx + frame_w, fy + frame_h), radius=RADIUS, outline=(ACCENT_DIM[0], ACCENT_DIM[1], ACCENT_DIM[2]), width=2)
    draw_status_bar(canvas, draw, fx, fy, frame_w)

    cap_y = H - CAPTION_H + 8
    draw_caption(draw, cap_y, caption, subtitle)

    canvas.save(out_path, "PNG", optimize=True)
    print(f"wrote {out_path} ({canvas.size})")


def draw_link_mark(size: int) -> Image.Image:
    """Crisp Fixaverse S-link mark for store icon."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    s = size
    stroke = max(16, s // 14)
    light = ACCENT + (255,)
    dark = (28, 88, 160, 255)
    outline = (220, 236, 255, 255)

    def line(a, b, fill):
        draw.line([a, b], fill=fill, width=stroke)

    pad = s * 0.18
    x0, y0 = pad, pad + s * 0.08
    x1, y1 = s - pad, s * 0.42
    x2, y2 = pad, s * 0.58
    x3, y3 = s - pad, s - pad - s * 0.08
    pts = [(x0, y0), (x1, y1), (x2, y2), (x3, y3)]
    for i in range(len(pts) - 1):
        draw.line([pts[i], pts[i + 1]], fill=outline, width=stroke + 4)
    line((x0, y0), (x1, y1), light)
    line((x1, y1), (x2, y2), dark)
    line((x2, y2), (x3, y3), light)
    r = stroke * 0.55
    for cx, cy in [(x1, y0), (x3, y3)]:
        draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=outline)
        draw.ellipse((cx - r * 0.55, cy - r * 0.55, cx + r * 0.55, cy + r * 0.55), fill=(*BG, 255))
    return img


def generate_icon(out_path: Path, mark_path: Path | None = None) -> None:
    size = 512
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)

    pad = 8
    draw.rounded_rectangle((pad, pad, size - pad, size - pad), radius=96, fill=(*BG, 255))
    draw.rounded_rectangle((pad + 4, pad + 4, size - pad - 4, size - pad - 4), radius=92, outline=(*ACCENT_DIM, 180), width=3)

    mark = draw_link_mark(int(size * 0.72))
    mx = (size - mark.width) // 2
    my = (size - mark.height) // 2 + 4
    canvas.paste(mark, (mx, my), mark)

    canvas.save(out_path, "PNG", optimize=True)
    print(f"wrote {out_path}")


def main() -> None:
    UPLOAD.mkdir(parents=True, exist_ok=True)

    compose_screenshot(
        ROOT / "smoke-uuid-08-wo-detail.png",
        UPLOAD / "screenshot-01.png",
        caption="Заявки",
        subtitle="Список работ и статусы на объекте",
        crop_box=(110, 0, 1750, 1300),
    )

    compose_screenshot(
        ROOT / "smoke-uuid-13-board.png",
        UPLOAD / "screenshot-02.png",
        caption="Доска",
        subtitle="План недели и заявки на линии",
        crop_box=(110, 0, 2400, 1300),
    )

    compose_screenshot(
        ROOT / "smoke-uuid-10-equipment.png",
        UPLOAD / "screenshot-03.png",
        caption="Оборудование",
        subtitle="Паспорт, документы и инвентарный номер",
        crop_box=(110, 0, 1900, 1300),
    )

    compose_screenshot(
        ROOT / "smoke-uuid-11-ppr.png",
        UPLOAD / "screenshot-04.png",
        caption="ППР",
        subtitle="Карты обслуживания и регламенты",
        crop_box=(110, 0, 2100, 1300),
    )

    # Raster sources in masterdoc*/landing are soft/pixelated; prefer crisp SVG mark.
    render_store_icon(OUT)


def render_store_icon(out_dir: Path) -> None:
    """Rasterize store/rustore/icon-mark.svg → icon-512(+opaque)."""
    from io import BytesIO

    svg_path = out_dir / "icon-mark.svg"
    if not svg_path.is_file():
        raise SystemExit(f"missing {svg_path}")
    try:
        import cairosvg
    except ImportError as exc:
        raise SystemExit("cairosvg required to rasterize icon-mark.svg") from exc

    raw = cairosvg.svg2png(url=str(svg_path), output_width=1024, output_height=1024)
    img = Image.open(BytesIO(raw)).convert("RGBA").resize((512, 512), Image.Resampling.LANCZOS)
    img.save(out_dir / "icon-512.png", "PNG")
    opaque = Image.new("RGB", (512, 512), BG)
    opaque.paste(img.convert("RGB"), mask=img.split()[-1])
    opaque.save(out_dir / "icon-512-opaque.png", "PNG")
    print(f"rasterized {svg_path.name} → icon-512.png / icon-512-opaque.png")


if __name__ == "__main__":
    main()
