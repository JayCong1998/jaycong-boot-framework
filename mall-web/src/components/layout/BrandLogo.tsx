import { Link } from "react-router-dom";
import { ShoppingBag } from "lucide-react";
export default function BrandLogo() {
  return (
    <Link to="/" className="brand" aria-label="简选商城首页">
      <span className="brand-symbol">
        <ShoppingBag size={27} strokeWidth={1.8} />
      </span>
      <span>
        简选商城<span className="brand-caption">精选好物 · 简单生活</span>
      </span>
    </Link>
  );
}
