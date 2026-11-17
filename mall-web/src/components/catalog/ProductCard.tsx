import type { Product } from "@/types";
import { ShoppingCart } from "lucide-react";
import { useCartStore } from "@/stores/cart";
import { useNoticeStore } from "@/stores/notice";
import { money } from "@/lib/utils";
export default function ProductCard({ product }: { product: Product }) {
  const add = useCartStore((s) => s.add),
    show = useNoticeStore((s) => s.show);
  return (
    <article className="product-card">
      <div className="product-image-wrap">
        <img
          src={product.image}
          alt={product.name}
          loading="lazy"
          onError={(e) => {
            if (
              !e.currentTarget.src.endsWith("/images/product-placeholder.svg")
            )
              e.currentTarget.src = "/images/product-placeholder.svg";
          }}
        />
      </div>
      <div className="product-info">
        <h3>{product.name}</h3>
        <p className="product-description">{product.description}</p>
        <div className="product-bottom">
          <div className="product-prices">
            <span className="price">
              <small>¥</small>
              {money(product.price)}
            </span>
          </div>
          <button
            className="add-cart"
            aria-label={`将${product.name}加入购物车`}
            onClick={() => {
              add(product);
              show(`已将「${product.name}」加入购物车`);
            }}
          >
            <ShoppingCart size={20} />
          </button>
        </div>
      </div>
    </article>
  );
}
