import { useEffect, useRef, useState } from "react";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
const slides = [
  {
    eyebrow: "简约生活 · 从这里开始",
    title: "品质好物  惊喜优惠",
    text: "精选优质商品，享受更好的生活",
    label: "立即抢购",
  },
  {
    eyebrow: "灵感办公 · 自在高效",
    title: "轻装上阵  自有不凡",
    text: "发现数码好物，让每一天更从容",
    label: "探索好物",
  },
  {
    eyebrow: "用心甄选 · 悦享日常",
    title: "生活小确幸  即刻拥有",
    text: "把喜欢带回家，让美好触手可及",
    label: "发现精选",
  },
];

export default function HeroBanner({ onShop }: { onShop: () => void }) {
  const [active, setActive] = useState(0);
  const hovered = useRef(false),
    focused = useRef(false);
  useEffect(() => {
    const timer = setInterval(() => {
      if (!hovered.current && !focused.current)
        setActive((n) => (n + 1) % slides.length);
    }, 6000);
    return () => clearInterval(timer);
  }, []);
  return (
    <section
      className="hero-banner"
      aria-label="精选活动"
      aria-roledescription="轮播"
      onMouseEnter={() => {
        hovered.current = true;
      }}
      onMouseLeave={() => {
        hovered.current = false;
      }}
      onFocus={() => {
        focused.current = true;
      }}
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget)) focused.current = false;
      }}
    >
      <img
        className="hero-image"
        src="/images/hero.png"
        alt="简约桌面上的轻薄笔记本与绿植"
        fetchPriority="high"
      />
      <div className="hero-shade"></div>
      <div className="hero-copy">
        <p className="hero-eyebrow">{slides[active]!.eyebrow}</p>
        <h1>{slides[active]!.title}</h1>
        <p className="hero-description">{slides[active]!.text}</p>
        <Button className="hero-cta" onClick={onShop}>
          {slides[active]!.label}
          <ArrowRight size={18} />
        </Button>
      </div>
      <div className="hero-dots">
        {slides.map((_, index) => (
          <button
            key={index}
            className={active === index ? "active" : ""}
            aria-label={`切换至第 ${index + 1} 张活动`}
            aria-pressed={active === index}
            onClick={() => setActive(index)}
          />
        ))}
      </div>
    </section>
  );
}
