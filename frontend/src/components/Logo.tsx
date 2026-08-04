import type { MouseEventHandler } from 'react';
import logo from '../assets/logo.webp';
import '../assets/css/Logo.css';

type LogoSize = 'small' | 'medium' | 'large' | 'xlarge';

interface LogoProps {
  size?: LogoSize;
  className?: string;
  onClick?: MouseEventHandler<HTMLDivElement>;
}

const SIZE_CLASSES: Record<LogoSize, string> = {
  small: 'logo-small',
  medium: 'logo-medium',
  large: 'logo-large',
  xlarge: 'logo-xlarge'
};

const Logo = ({
  size = 'medium',
  className = '',
  onClick
}: LogoProps) => {
  const logoClasses = `logo ${SIZE_CLASSES[size]} ${className}`.trim();

  return (
    <div className={logoClasses} onClick={onClick}>
      <img
        src={logo}
        alt="Financial Manager Logo"
        className="logo-image"
      />
    </div>
  );
};

export default Logo;
