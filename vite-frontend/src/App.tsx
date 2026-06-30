import { Route, Routes, useNavigate } from "react-router-dom";
import { Suspense, lazy, useEffect, useState } from "react";

import { isAdmin, isLoggedIn } from "@/utils/auth";
import { siteConfig } from "@/config/site";

const IndexPage = lazy(() => import("@/pages/index"));
const ChangePasswordPage = lazy(() => import("@/pages/change-password"));
const DashboardPage = lazy(() => import("@/pages/dashboard"));
const ForwardPage = lazy(() => import("@/pages/forward"));
const TunnelPage = lazy(() => import("@/pages/tunnel"));
const NodePage = lazy(() => import("@/pages/node"));
const ProfilePage = lazy(() => import("@/pages/profile"));
const LimitPage = lazy(() => import("@/pages/limit"));
const ConfigPage = lazy(() => import("@/pages/config"));
const SettingsPage = lazy(() => import("@/pages/settings").then(m => ({ default: m.SettingsPage })));

const AdminLayout = lazy(() => import("@/layouts/admin"));
const H5Layout = lazy(() => import("@/layouts/h5"));
const H5SimpleLayout = lazy(() => import("@/layouts/h5-simple"));

const PageLoader = () => (
  <div className="flex items-center justify-center min-h-screen bg-white dark:bg-black">
    <div className="text-lg text-gray-700 dark:text-gray-200">加载中...</div>
  </div>
);

// 检测是否为H5模式
const useH5Mode = () => {
  const getInitialH5Mode = () => {
    const isMobile = window.innerWidth <= 768;
    const isMobileBrowser = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    const urlParams = new URLSearchParams(window.location.search);
    const isH5Param = urlParams.get('h5') === 'true';
    
    return isMobile || isMobileBrowser || isH5Param;
  };

  const [isH5, setIsH5] = useState(getInitialH5Mode);

  useEffect(() => {
    const checkH5Mode = () => {
      const isMobile = window.innerWidth <= 768;
      const isMobileBrowser = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
      const urlParams = new URLSearchParams(window.location.search);
      const isH5Param = urlParams.get('h5') === 'true';
      
      setIsH5(isMobile || isMobileBrowser || isH5Param);
    };

    window.addEventListener('resize', checkH5Mode);
    
    return () => window.removeEventListener('resize', checkH5Mode);
  }, []);

  return isH5;
};

const ProtectedRoute = ({ children, useSimpleLayout = false, skipLayout = false }: { children: React.ReactNode, useSimpleLayout?: boolean, skipLayout?: boolean }) => {
  const authenticated = isLoggedIn();
  const isH5 = useH5Mode();
  const navigate = useNavigate();
  
  useEffect(() => {
    if (!authenticated) {
      navigate('/', { replace: true });
    }
  }, [authenticated, navigate]);

  if (!authenticated) {
    return <PageLoader />;
  }

  if (skipLayout) {
    return <>{children}</>;
  }

  let Layout;
  if (isH5 && useSimpleLayout) {
    Layout = H5SimpleLayout;
  } else if (isH5) {
    Layout = H5Layout;
  } else {
    Layout = AdminLayout;
  }
  
  return (
    <Suspense fallback={<PageLoader />}>
      <Layout>{children}</Layout>
    </Suspense>
  );
};

const AdminRoute = ({ children, useSimpleLayout = false }: { children: React.ReactNode, useSimpleLayout?: boolean }) => {
  const navigate = useNavigate();
  const authorized = isLoggedIn() && isAdmin();

  useEffect(() => {
    if (!authorized) {
      navigate('/dashboard', { replace: true });
    }
  }, [authorized, navigate]);

  if (!authorized) {
    return <PageLoader />;
  }

  return (
    <ProtectedRoute useSimpleLayout={useSimpleLayout}>
      {children}
    </ProtectedRoute>
  );
};

const LoginRoute = () => {
  const authenticated = isLoggedIn();
  const navigate = useNavigate();
  
  useEffect(() => {
    if (authenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [authenticated, navigate]);
  
  if (authenticated) {
    return <PageLoader />;
  }
  
  return (
    <Suspense fallback={<PageLoader />}>
      <IndexPage />
    </Suspense>
  );
};

function App() {
  useEffect(() => {
    document.title = siteConfig.name;
    
    const checkTitleUpdate = async () => {
      try {
        const { getCachedConfig } = await import('@/config/site');
        const cachedAppName = await getCachedConfig('app_name');
        if (cachedAppName && cachedAppName !== document.title) {
          document.title = cachedAppName;
        }
      } catch (error) {
        console.warn('检查标题更新失败:', error);
      }
    };

    const timer = setTimeout(checkTitleUpdate, 100);

    return () => clearTimeout(timer);
  }, []);

  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/" element={<LoginRoute />} />
        <Route 
          path="/change-password" 
          element={
            <ProtectedRoute skipLayout={true}>
              <ChangePasswordPage />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/forward" 
          element={
            <ProtectedRoute>
              <ForwardPage />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/tunnel" 
          element={
            <AdminRoute>
              <TunnelPage />
            </AdminRoute>
          } 
        />
        <Route 
          path="/node" 
          element={
            <AdminRoute>
              <NodePage />
            </AdminRoute>
          } 
        />
        <Route 
          path="/profile" 
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/limit" 
          element={
            <AdminRoute useSimpleLayout={true}>
              <LimitPage />
            </AdminRoute>
          } 
        />
        <Route 
          path="/config" 
          element={
            <AdminRoute useSimpleLayout={true}>
              <ConfigPage />
            </AdminRoute>
          } 
        />
        <Route 
          path="/settings" 
          element={
            <ProtectedRoute useSimpleLayout={true}>
              <SettingsPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Suspense>
  );
}

export default App;
