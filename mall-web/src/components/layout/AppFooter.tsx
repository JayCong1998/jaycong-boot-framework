import { ShieldCheck, Truck, Headphones } from "lucide-react";
export default function AppFooter() {
  return (
    <footer className="site-footer">
      <div className="page-container">
        <div className="footer-promises">
          <span>
            <ShieldCheck size={17} />
            品质甄选
          </span>
          <span>
            <Truck size={18} />
            用心送达
          </span>
          <span>
            <Headphones size={17} />
            贴心服务
          </span>
        </div>
        <p>
          © {new Date().getFullYear()} 简选商城
          <span className="footer-dot">·</span>让生活更简单
        </p>
      </div>
    </footer>
  );
}
