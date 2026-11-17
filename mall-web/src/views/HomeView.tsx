import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { SearchX } from "lucide-react";
import CategoryNav from "@/components/catalog/CategoryNav";
import HeroBanner from "@/components/catalog/HeroBanner";
import ProductCard from "@/components/catalog/ProductCard";
import { Button } from "@/components/ui/button";
import { fetchGoods } from "@/services/goods";
import type { Product } from "@/types";
export default function HomeView() {
  const location = useLocation(),
    navigate = useNavigate();
  const query = (new URLSearchParams(location.search).get("q") || "").trim();
  const [page, setPage] = useState(1),
    [expanded, setExpanded] = useState(false),
    [retry, setRetry] = useState(0);
  const [data, setData] = useState<{
    products: Product[];
    total: number;
    loading: boolean;
    error: string;
  }>({ products: [], total: 0, loading: true, error: "" });
  const [previousQuery, setPreviousQuery] = useState(query);
  if (previousQuery !== query) {
    setPreviousQuery(query);
    setPage(1);
    setExpanded(true);
  }
  const size = 20;
  useEffect(() => {
    let current = true;
    setData((d) => ({ ...d, loading: true, error: "" }));
    fetchGoods(page, size, query).then(
      (result) => {
        if (current) setData({ ...result, loading: false, error: "" });
      },
      (cause) => {
        if (current)
          setData({
            products: [],
            total: 0,
            loading: false,
            error: (cause as Error).message,
          });
      },
    );
    return () => {
      current = false;
    };
  }, [page, query, retry]);
  const { products, total, loading, error } = data;
  const visible = expanded || query ? products : products.slice(0, 5);
  function shop() {
    setExpanded(true);
    document
      .getElementById("products")
      ?.scrollIntoView({ behavior: "smooth", block: "start" });
  }
  return (
    <div className="page-container home-page">
      <div className="hero-layout">
        <CategoryNav selected="all" onSelect={shop} />
        <HeroBanner onShop={shop} />
      </div>
      <section
        id="products"
        className="products-section"
        aria-labelledby="products-title"
      >
        <div className="section-heading">
          <h2 id="products-title">
            {query ? `“${query}” 的搜索结果` : "热门推荐"}
          </h2>
          {!expanded && !query ? (
            <button className="view-more" onClick={() => setExpanded(true)}>
              查看更多
            </button>
          ) : (
            <span className="results-count">共 {total} 件好物</span>
          )}
        </div>
        {loading ? (
          <div className="empty-state" role="status">
            正在加载商品…
          </div>
        ) : error ? (
          <div className="empty-state" role="alert">
            <h3>商品加载失败</h3>
            <p>{error}</p>
            <Button variant="outline" onClick={() => setRetry((n) => n + 1)}>
              重新加载
            </Button>
          </div>
        ) : visible.length ? (
          <div className="product-grid">
            {visible.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <SearchX size={43} />
            <h3>暂时没有找到相关商品</h3>
            <p>换个关键词，或稍后再来看看吧</p>
            <Button variant="outline" onClick={() => navigate("/")}>
              浏览全部商品
            </Button>
          </div>
        )}
        {!loading && !error && total > size && (expanded || query) ? (
          <nav className="goods-pagination" aria-label="商品分页">
            <Button
              variant="outline"
              disabled={page <= 1}
              onClick={() => setPage((n) => n - 1)}
            >
              上一页
            </Button>
            <span>
              第 {page} / {Math.ceil(total / size)} 页
            </span>
            <Button
              variant="outline"
              disabled={page * size >= total}
              onClick={() => setPage((n) => n + 1)}
            >
              下一页
            </Button>
          </nav>
        ) : null}
      </section>
    </div>
  );
}
